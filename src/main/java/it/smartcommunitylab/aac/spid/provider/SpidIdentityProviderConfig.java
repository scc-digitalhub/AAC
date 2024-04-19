/*
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.spid.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.crypto.CertificateParser;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.UsageType;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SpidIdentityProviderConfig extends AbstractIdentityProviderConfig<SpidIdentityProviderConfigMap> {

    public static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + SpidIdentityProviderConfigMap.RESOURCE_TYPE;

    //    public static final String DEFAULT_METADATA_URL =
    //        "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "metadata/{registrationId}";
    //    public static final String DEFAULT_CONSUMER_URL =
    //        "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "sso/{registrationId}";
    //    public static final String DEFAULT_LOGOUT_URL =
    //        "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "slo/{registrationId}";

    private transient Set<RelyingPartyRegistration> relyingPartyRegistrations; // first time evaluated by the getter, then immutable
    private Map<String, SpidRegistration> identityProviders; // local registry

    public SpidIdentityProviderConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_SPID, provider, realm);
    }

    public SpidIdentityProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new IdentityProviderSettingsMap(), new SpidIdentityProviderConfigMap());
        this.relyingPartyRegistrations = null;
        this.identityProviders = Collections.emptyMap(); // TODO: check later on if this should be passed in the constructor
    }

    public SpidIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        IdentityProviderSettingsMap settings,
        SpidIdentityProviderConfigMap configs
    ) {
        super(cp, settings, configs);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    private SpidIdentityProviderConfig() {
        super();
    }

    /*
     * Extract a provider from a registration with pattern either {providerId}
     * or {providerId}|{idpKey} where idpKey is the key of the upstream
     * SPID identity provider.
     */
    public static String getProviderId(String decodedRegistrationId) {
        Assert.hasText(decodedRegistrationId, "registrationId can not be blank");

        // registrationId is {providerId}|{idpKey}
        String[] kp = StringUtils.split(decodedRegistrationId, "|");
        if (kp == null) {
            return decodedRegistrationId;
        }
        //kp[0], kp[1] = providerId, idpKey
        return kp[0];
    }

    public Map<String, SpidRegistration> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(Collection<SpidRegistration> idpRegs) {
        if (idpRegs != null) {
            this.identityProviders =
                idpRegs.stream().collect(Collectors.toMap(SpidRegistration::getEntityId, reg -> reg));
        }
    }

    public String getEntityId() {
        return configMap.getEntityId() != null ? configMap.getEntityId() : getMetadataUrl();
    }

    @JsonIgnore
    public Set<RelyingPartyRegistration> getRelyingPartyRegistrations() {
        if (relyingPartyRegistrations == null) {
            try {
                relyingPartyRegistrations = toRelyingPartyRegistrations();
            } catch (IOException | CertificateException e) {
                throw new RuntimeException("error building registration: " + e.getMessage());
            }
        }
        return relyingPartyRegistrations;
    }

    @JsonIgnore
    public Set<RelyingPartyRegistration> getUpstreamRelyingPartyRegistrations() {
        Set<RelyingPartyRegistration> regs = getRelyingPartyRegistrations();
        return regs
            .stream()
            .filter(reg -> !reg.getRegistrationId().equals(getMetadataRegistrationId()))
            .collect(Collectors.toSet());
    }

    // generate a registration (an RP/AP pair as defined by OpenSaml) for _each_
    // configured upstream idps
    private Set<RelyingPartyRegistration> toRelyingPartyRegistrations() throws IOException, CertificateException {
        Set<RelyingPartyRegistration> registrations = new HashSet<>();
        try {
            Set<String> idpMetadataUrls = getAssertingPartyMetadataUrls();
            for (String idpMetadataUrl : idpMetadataUrls) {
                registrations.add(toRelyingPartyRegistration(idpMetadataUrl));
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("spid provider failed to invalid metadata uri: " + e.getMessage());
        }

        if (registrations.isEmpty()) {
            throw new IllegalArgumentException(
                "invalid configuration: failed to acquire any relaying party registration for spid provider " +
                getProvider()
            );
        }

        // add a global registration for metadata
        RelyingPartyRegistration metadataRegistration = RelyingPartyRegistration
            .withRelyingPartyRegistration(registrations.iterator().next())
            .registrationId(getMetadataRegistrationId())
            .build();
        registrations.add(metadataRegistration);

        return registrations;
    }

    // returns the list of metadata urls of upstream idps
    private Set<String> getAssertingPartyMetadataUrls() {
        Set<String> idpMetadataUrls = new HashSet<>();

        // idp urls must be defined by either in configMap or application config (and passed to this.idps)
        if (StringUtils.hasText(configMap.getIdpMetadataUrl())) {
            // single idp via url
            idpMetadataUrls = Collections.singleton(configMap.getIdpMetadataUrl());
            return idpMetadataUrls;
        }
        if (identityProviders == null) {
            throw new IllegalArgumentException("invalid configuration: no defined upstream spid idps");
        }
        // if config map defined some specific idps, pick those, otherwise pick all
        if (configMap.getIdps() != null && !configMap.getIdps().isEmpty()) {
            for (String idp : configMap.getIdps()) {
                SpidRegistration reg = identityProviders.get(idp);
                if (reg != null) {
                    idpMetadataUrls.add(reg.getMetadataUrl());
                }
            }
        } else {
            for (SpidRegistration reg : identityProviders.values()) {
                idpMetadataUrls.add(reg.getMetadataUrl());
            }
        }
        if (idpMetadataUrls.isEmpty()) {
            throw new IllegalArgumentException("invalid configuration: no defined upstream spid idps");
        }
        return idpMetadataUrls;
    }

    // yield a unique key per upstream metadata (url)
    // the key is fetched from configuration file (is present), otherwise host is used
    // the function might throws an exception if the provided the metadata url is not
    // a valid uri
    public String evalIdpKeyIdentifier(String idpMetadataUrl) throws URISyntaxException {
        Optional<SpidRegistration> reg =
            this.identityProviders.values().stream().filter(r -> r.getMetadataUrl().equals(idpMetadataUrl)).findFirst();
        if (reg.isPresent()) {
            return reg.get().getEntityLabel();
            //            return reg.get().getEntityId();
        }
        return new URI(idpMetadataUrl).getHost();
    }

    // obtain an allegedly unique identifier from an idp key; this identifier can be used
    // to identify a relying party registration
    private String evalRelyingPartyRegistrationId(String idpKeyIdentifier) {
        // NOTE: this function is 'inverted' by getProviderId(..)
        return getProvider() + "|" + idpKeyIdentifier;
    }

    public String getConsumerUrl() {
        return "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "sso/" + getMetadataRegistrationId();
    }

    public String getLogoutUrl() {
        return "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "slo/" + getMetadataRegistrationId();
    }

    public String getMetadataUrl() {
        return "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "metadata/" + getMetadataRegistrationId();
    }

    // create a relying party registration for an upstream idp; only ap autoconfiguration
    // is supported, hence function parameters require an idp metadata url
    private RelyingPartyRegistration toRelyingPartyRegistration(String idpMetadataUrl)
        throws IOException, CertificateException, URISyntaxException {
        // start from ap autoconfiguration ...
        String key = evalIdpKeyIdentifier(idpMetadataUrl);
        String registrationId = encodeRegistrationId(evalRelyingPartyRegistrationId(key));
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistrations
            .fromMetadataLocation(idpMetadataUrl)
            .registrationId(registrationId);

        // ... then expand with rp configuration (i.e. ourself)
        builder
            .entityId(getEntityId())
            .assertionConsumerServiceLocation(getConsumerUrl())
            .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
            .singleLogoutServiceLocation(getLogoutUrl());

        String signingKey = configMap.getSigningKey();
        String signingCertificate = configMap.getSigningCertificate();
        if (StringUtils.hasText(signingKey) && StringUtils.hasText(signingCertificate)) {
            // only RSA keys are supported
            Saml2X509Credential credential = CertificateParser.genCredentials(
                signingKey,
                signingCertificate,
                Saml2X509Credential.Saml2X509CredentialType.SIGNING,
                Saml2X509Credential.Saml2X509CredentialType.DECRYPTION
            );
            builder.signingX509Credentials(c -> c.add(credential));
            builder.decryptionX509Credentials(c -> c.add(credential));
        }

        return builder.build();
    }

    public X509Certificate getSigningCertificate() throws CertificateException, IOException {
        return CertificateParser.parseX509(configMap.getSigningCertificate());
    }

    public PrivateKey getSigningKey() throws IOException {
        return CertificateParser.parsePrivateWithUndefinedHeader(configMap.getSigningKey());
    }

    public List<Credential> getRelyingPartySigningCredentials() {
        List<Credential> credentials = new ArrayList<>();
        RelyingPartyRegistration rp = getRelyingPartyRegistrations().stream().findFirst().orElse(null);
        if (rp == null) {
            return credentials;
        }
        for (Saml2X509Credential x509Credential : rp.getSigningX509Credentials()) {
            X509Certificate certificate = x509Credential.getCertificate();
            PrivateKey privateKey = x509Credential.getPrivateKey();
            BasicCredential credential = CredentialSupport.getSimpleCredential(certificate, privateKey);
            credential.setEntityId(rp.getEntityId());
            credential.setUsageType(UsageType.SIGNING);
            credentials.add(credential);
        }
        return credentials;
    }

    // additional properties not supported by stock model
    public Boolean getRelyingPartyRegistrationIsForceAuthn() {
        // According to specs, ForceAuthn cannot be chosen for SpidL2 or SpidL3

        // return always true due to check in spid validator
        return true;
    }

    public Set<SpidAttribute> getSpidAttributes() {
        return configMap.getSpidAttributes() == null ? Collections.emptySet() : configMap.getSpidAttributes();
    }

    public Set<String> getRelyingPartyRegistrationAuthnContextClassRefs() {
        return configMap.getAuthnContext() == null
            ? Collections.emptySet()
            : Collections.singleton(configMap.getAuthnContext().getValue());
    }

    public SpidUserAttribute getSubAttributeName() {
        return configMap.getSubAttributeName();
    }

    public SpidUserAttribute getUsernameAttributeName() {
        return configMap.getUsernameAttributeName();
    }

    public String getIdpKey(String idpMetadataUrl) throws URISyntaxException {
        // check if registration
        Optional<SpidRegistration> reg = identityProviders
            .values()
            .stream()
            .filter(r -> r.getMetadataUrl().equals(idpMetadataUrl))
            .findFirst();
        if (reg.isPresent()) {
            return reg.get().getEntityLabel();
        }

        // extract name from url
        URI uri = new URI(idpMetadataUrl);
        return uri.getHost();
    }

    public Set<String> getRelyingPartyRegistrationIds() {
        Set<String> idpMetadataUrls = getAssertingPartyMetadataUrls();
        Set<String> ids = idpMetadataUrls
            .stream()
            .map(u -> {
                try {
                    return evalRelyingPartyRegistrationId(getIdpKey(u));
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("invalid metadata uri " + e.getMessage());
                }
            })
            .collect(Collectors.toSet());
        ids.add(getMetadataRegistrationId());
        return ids;
    }

    private String getMetadataRegistrationId() {
        return encodeRegistrationId(getProvider());
    }

    /*
     * getRelyingPartyRegistration yields a _single_ relying party registration with id equals to providerId.
     * This is required for cases where the registration does not require any asserting party details,
     * such as SPID metadata.
     */
    public RelyingPartyRegistration getRelyingPartyRegistration() {
        return getRelyingPartyRegistrations()
            .stream()
            .findAny()
            .map(r ->
                RelyingPartyRegistration
                    .withRelyingPartyRegistration(r)
                    .registrationId(getMetadataRegistrationId())
                    .build()
            )
            .orElse(null);
    }

    public static String encodeRegistrationId(String regId) {
        //        return URLEncoder.encode(regId, StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().encodeToString(regId.getBytes());
    }

    public static String decodeRegistrationId(String encodedRegId) {
        //        return URLDecoder.decode(encodedRegId, StandardCharsets.UTF_8);
        return new String(Base64.getUrlDecoder().decode(encodedRegId), StandardCharsets.UTF_8);
    }
}
