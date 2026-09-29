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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.NamespaceContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.model.SpidPurpose;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAttributeConsumingService;
import it.smartcommunitylab.aac.spid.model.SpidMetadataConfiguration;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;

public class SpidIdentityProviderConfig extends AbstractIdentityProviderConfig<SpidIdentityProviderConfigMap> {

    public static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + SpidIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final String XPATH_EXPRESSION_ENTITY_ID = "/md:EntityDescriptor/@entityID";
    private static final String XPATH_EXPRESSION_ORGANIZATION_NAME = "/md:EntityDescriptor/md:Organization/md:OrganizationName/text()";
    private static final String XPATH_EXPRESSION_ORGANIZATION_DISPLAY_NAME = "/md:EntityDescriptor/md:Organization/md:OrganizationDisplayName/text()";
    private static final String XPATH_EXPRESSION_ORGANIZATION_URL = "/md:EntityDescriptor/md:Organization/md:OrganizationURL/text()";
    private static final String XPATH_EXPRESSION_CONTACT_PERSON_EMAIL_ADDRESS = "/md:EntityDescriptor/md:ContactPerson/md:EmailAddress/text()";
    private static final String XPATH_EXPRESSION_CONTACT_PERSON_IPA_CODE = "/md:EntityDescriptor/md:ContactPerson/md:Extensions/spid:IPACode/text()";
    private static final String XPATH_EXPRESSION_ATTRIBUTE_CONSUMING_SERVICES = "/md:EntityDescriptor//md:AttributeConsumingService";
    private static final String XPATH_EXPRESSION_ACS_SERVICE_NAME = "md:ServiceName/text()";
    private static final String XPATH_EXPRESSION_ACS_REQUESTED_ATTRIBUTE = "md:RequestedAttribute";
    private static final String XPATH_EXPRESSION_SIGNING_CERTIFICATES = "/md:EntityDescriptor//md:KeyDescriptor[@use='signing']//ds:X509Certificate";

    private static final int DEFAULT_TIMEOUT = 10000;
    private static final int DEFAULT_ATTRIBUTE_CONSUMING_SERVICE_INDEX = 0;
    private static final String DEFAULT_ATTRIBUTE_CONSUMING_SERVICE_NAME = "default";

    private transient Set<RelyingPartyRegistration> relyingPartyRegistrations;
    private transient RelyingPartyRegistration metadataRelyingPartyRegistration;

    private Set<SpidRegistration> identityProviders;
    private SpidMetadataConfiguration metadataConfiguration;

    private transient SpidIdentityProviderStatusMap statusMap;
    private String baseUrl;

    private transient RestTemplate restTemplate;

    public SpidIdentityProviderConfig(String provider, String realm) {
        super(
            SystemKeys.AUTHORITY_SPID,
            provider,
            realm,
            new IdentityProviderSettingsMap(),
            new SpidIdentityProviderConfigMap()
        );
        this.relyingPartyRegistrations = null;
        this.metadataRelyingPartyRegistration = null;
        this.identityProviders = Collections.emptySet();
        this.metadataConfiguration = null;
        this.restTemplate = null;
    }

    public SpidIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        IdentityProviderSettingsMap settings,
        SpidIdentityProviderConfigMap configs
    ) {
        super(cp, settings, configs);
        validateSigningCredentials();
        loadMetadataConfiguration();
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private SpidIdentityProviderConfig() {
        super();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public SpidIdentityProviderStatusMap getStatusMap() {
        if (statusMap == null) {
            statusMap = new SpidIdentityProviderStatusMap();
            statusMap.setMetadataUrl(getMetadataUrl());
            statusMap.setAssertionConsumerUrl(getAssertionConsumerUrl());
            statusMap.setMetadataConfiguration(getMetadataConfiguration());
        }
        return statusMap;
    }

    public String metadataUrlTemplate() {
        return "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "metadata/{registrationId}";
    }

    public String getMetadataUrl() {
        if (baseUrl != null) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(metadataUrlTemplate());
            return builder.buildAndExpand(Map.of("baseUrl", baseUrl, "registrationId", getMetadataRegistrationId())).toUriString();
        }
        return null;
    }

    public String assertionConsumerUrlTemplate() {
        return "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "sso/{registrationId}";
    }

    public String getAssertionConsumerUrl(){
        if (baseUrl != null) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(assertionConsumerUrlTemplate());
            return builder.buildAndExpand(Map.of("baseUrl", baseUrl, "registrationId", getMetadataRegistrationId())).toUriString();
        }
        return null;
    }

    public Map<String, String> getMetadataConfiguration() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("entityId", getEntityId());
        config.put("organizationName", getOrganizationName());
        config.put("organizationDisplayName", getOrganizationDisplayName());
        config.put("organizationUrl", getOrganizationUrl());
        config.put("contactPersonEmailAddress", getContactPersonEmailAddress());
        config.put("contactPersonIpaCode", getContactPersonIPACode());

        getAttributeConsumingServices()
            .stream()
            .filter(acs -> acs.getIndex() == getAttributeConsumingServiceIndex())
            .findFirst()
            .ifPresent(acs -> config.put(
                "attributeConsumingServiceIndex " + acs.getIndex(),
                acs.getAttributes()
                    .stream()
                    .map(SpidAttribute::toString)
                    .sorted()
                    .collect(Collectors.joining(", "))
            ));

        return config;
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

    public Set<SpidRegistration> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(Collection<SpidRegistration> localIdpRegistry) {
        Set<SpidRegistration> identityProviders = new HashSet<>();

        // identity providers must be defined either in the configMap via url or in the application configuration (local registry)
        if (StringUtils.hasText(configMap.getIdpMetadataUrl())) {
            // single idp via url
            SpidRegistration reg = null;

            if (localIdpRegistry != null) {
                reg = localIdpRegistry
                    .stream()
                    .filter(idp -> idp.getMetadataUrl().equals(configMap.getIdpMetadataUrl()))
                    .findFirst().orElse(null);
            } 
            if (reg == null) {
                try {
                    // build its relying party registration and create a default SpidRegistration
                    RelyingPartyRegistration apReg = toRelyingPartyRegistration(configMap.getIdpMetadataUrl(), configMap.getIdpMetadataUrl(), SigningCredentialHelper.CredentialPurpose.AUTH_REQUEST);
                    String idpEntityId = apReg.getAssertingPartyDetails().getEntityId();

                    reg = new SpidRegistration();
                    reg.setEntityId(idpEntityId);
                    reg.setEntityName(idpEntityId);
                    reg.setMetadataUrl(configMap.getIdpMetadataUrl());
                    reg.setEntityLabel(idpEntityId);
                } catch (IOException | CertificateException e) {
				    // skip that registration if that idp is offline
			    }
            }
            if (reg != null) {
                identityProviders.add(reg);
            }
        } else if (localIdpRegistry != null) {
            // multiple idps via local registry
            // if config map defined some specific idps, pick those, otherwise pick all
            if (configMap.getIdps() != null && !configMap.getIdps().isEmpty()) {
                localIdpRegistry.stream()
                    .filter(idp -> configMap.getIdps().contains(idp.getEntityId()))
                    .forEach(idp -> identityProviders.add(idp));
            } else {
                identityProviders.addAll(localIdpRegistry);
            }
        }

        if (identityProviders.isEmpty()) {
            throw new IllegalArgumentException("invalid configuration: no defined upstream spid idps");
        }

        this.identityProviders = identityProviders;
    }

    /*
     * getRelyingPartyRegistration yields a single relying party registration 
     * with registration id equal to the encoded providerId.
     * This is required for cases where the registration does not require any asserting party details,
     * such as SPID metadata.
     */
    @JsonIgnore
    public RelyingPartyRegistration getRelyingPartyRegistration() {
        if (relyingPartyRegistrations != null) {
            String registrationId = getMetadataRegistrationId();
            RelyingPartyRegistration r = relyingPartyRegistrations
                .stream()
                .filter(reg -> reg.getRegistrationId().equals(registrationId))
                .findFirst().orElse(null);

            if (r != null) {
                return r;
            }
        }

        try {
            RelyingPartyRegistration r = toBareRelyingPartyRegistration(SigningCredentialHelper.CredentialPurpose.AUTH_REQUEST);
            if (relyingPartyRegistrations == null) {
                relyingPartyRegistrations = new HashSet<>();
            }
            relyingPartyRegistrations.add(r);
            return r;
        } catch (IOException | CertificateException e) {
            throw new RuntimeException("error building registration: " + e.getMessage());
        }
    }

    /*
     * getRelyingPartyRegistration yields a single relying party registration 
     * for the configured upstream identity provider associated to the input idpKey
     */
    @JsonIgnore
    public RelyingPartyRegistration getRelyingPartyRegistration(String idpKey) {
        if (relyingPartyRegistrations != null) {
            String registrationId = encodeRegistrationId(evalRelyingPartyRegistrationId(idpKey));
            RelyingPartyRegistration r = relyingPartyRegistrations
                .stream()
                .filter(reg -> reg.getRegistrationId().equals(registrationId))
                .findFirst().orElse(null);

            if (r != null) {
                return r;
            }
        }

        try {
            String idpMetadataUrl = getAssertingPartyMetadataUrl(idpKey);
            String registrationId = encodeRegistrationId(evalRelyingPartyRegistrationId(evalIdpKeyIdentifier(idpMetadataUrl)));
            RelyingPartyRegistration r = toRelyingPartyRegistration(registrationId, idpMetadataUrl, SigningCredentialHelper.CredentialPurpose.AUTH_REQUEST);
            if (relyingPartyRegistrations == null) {
                relyingPartyRegistrations = new HashSet<>();
            }
            relyingPartyRegistrations.add(r);
            return r;
        } catch (IOException | CertificateException | URISyntaxException e) {
            throw new RuntimeException("error building registration: " + e.getMessage());
        }
    }

    /*
     * getMetadataRelyingPartyRegistration provides a single relying party registration
     * with registration id equal to the encoded providerId.
     * This is used for SPID metadata where asserting party details are not required.
     * It differs from getRelyingPartyRegistration as it builds a registration
     * using the full set of signing certificates instead of the single active one,
     * ensuring valid metadata during rotations.
     */
    @JsonIgnore
    public RelyingPartyRegistration getMetadataRelyingPartyRegistration() {
        if (metadataRelyingPartyRegistration != null) {
            String registrationId = getMetadataRegistrationId();
            if (metadataRelyingPartyRegistration.getRegistrationId().equals(registrationId)) {
                return metadataRelyingPartyRegistration;
            }
        }

        try {
            // Build the "bare" registration (single/provider-wide)
            RelyingPartyRegistration r = toBareRelyingPartyRegistration(SigningCredentialHelper.CredentialPurpose.METADATA_EXPOSURE);
            this.metadataRelyingPartyRegistration = r;
            return r;
        } catch (IOException | CertificateException e) {
            throw new RuntimeException("error building registration: " + e.getMessage());
        }
    }

    // create a relying party registration with placeholder ap configuration.
    private RelyingPartyRegistration toBareRelyingPartyRegistration(SigningCredentialHelper.CredentialPurpose purpose) throws IOException, CertificateException {
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration
            .withRegistrationId(getMetadataRegistrationId())
            .entityId(getEntityId())
            .assertionConsumerServiceLocation(getConsumerUrl())
            .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
            .singleLogoutServiceLocation(getLogoutUrl());
        
        builder.assertingPartyDetails(party -> 
            party
                .entityId("http://placeholder.entity.id")
                .singleSignOnServiceLocation("http://placeholder.sso.location")
        );

        builder = buildSigningCredentials(builder, purpose);
        return builder.build();
    }

    // create a relying party registration for an upstream idp; only ap autoconfiguration is supported,
    // hence function parameters require an idp metadata url
    private RelyingPartyRegistration toRelyingPartyRegistration(String registrationId, String idpMetadataUrl, SigningCredentialHelper.CredentialPurpose purpose) throws IOException, CertificateException {
        // start from ap autoconfiguration ...
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistrations
            .fromMetadataLocation(idpMetadataUrl)
            .registrationId(registrationId);

        // ... then expand with rp configuration (i.e. ourself)
        builder
            .entityId(getEntityId())
            .assertionConsumerServiceLocation(getConsumerUrl())
            .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
            .singleLogoutServiceLocation(getLogoutUrl());

        builder = buildSigningCredentials(builder, purpose);
        return builder.build();
    }

    private RelyingPartyRegistration.Builder buildSigningCredentials(RelyingPartyRegistration.Builder builder, SigningCredentialHelper.CredentialPurpose purpose) throws IOException, CertificateException {

        List<SigningCredential> signingCredentialList = SigningCredentialHelper.signingCredentialList(configMap, purpose);

        for (SigningCredential signingCredential : signingCredentialList) {
            if (StringUtils.hasText(signingCredential.getSigningKey()) && StringUtils.hasText(signingCredential.getSigningCertificate())) {
                // only RSA keys are supported
                Saml2X509Credential credential = SigningCredentialHelper.createSaml2X509Credential(
                    signingCredential.getSigningKey(),
                    signingCredential.getSigningCertificate());
                builder.signingX509Credentials(c -> c.add(credential));
            }
        }

        return builder;
    }

    // Performs an upfront validation of all SPID cryptographic contexts to prevent runtime failures.
    private void validateSigningCredentials() {
        try {
            // 1. Validate AUTH_REQUEST
            List<SigningCredential> authRequestCredentialList = SigningCredentialHelper.signingCredentialList(configMap, SigningCredentialHelper.CredentialPurpose.AUTH_REQUEST);
            ensureNotEmpty(authRequestCredentialList);

            // 2. Validate METADATA_SIGNATURE
            List<SigningCredential> metadataSignatureCredentialList = SigningCredentialHelper.signingCredentialList(configMap, SigningCredentialHelper.CredentialPurpose.METADATA_SIGNATURE);
            ensureNotEmpty(metadataSignatureCredentialList);

            // 3. Validate METADATA_EXPOSURE
            List<SigningCredential> metadataExposureCredentialList = SigningCredentialHelper.signingCredentialList(configMap, SigningCredentialHelper.CredentialPurpose.METADATA_EXPOSURE);
            ensureNotEmpty(metadataExposureCredentialList);

            Set<String> seenCertificates = new HashSet<>();
            for (SigningCredential signingCredential : metadataExposureCredentialList) {
                if (StringUtils.hasText(signingCredential.getSigningKey()) && StringUtils.hasText(signingCredential.getSigningCertificate())) {
                    String normalizedCert = signingCredential.getSigningCertificate().replaceAll("\\s+", "");
                    if (!seenCertificates.add(normalizedCert)) {
                        throw new IllegalArgumentException("CRITICAL: Duplicate certificates found in the METADATA_EXPOSURE list!");
                    }
                }
            }
        } catch (IOException | CertificateException e) {
            throw new IllegalArgumentException("Failed to validate SigningCredentials: " + e.getMessage(), e);
        }
    }

    // If no valid credentials were found/processed,
    private void ensureNotEmpty(List<SigningCredential> signingCredentialList) {
        if (signingCredentialList.isEmpty()) {
            throw new IllegalArgumentException("CRITICAL: Missing SPID signing credentials. " +
                "The Service Provider cannot establish a Circle of Trust with IdPs " +
                "as required by AgID technical regulations (Binding HTTP-POST/Redirect).");
        }
    }

    // yield a unique key per upstream metadata (url)
    // the key is fetched from configuration file (is present), otherwise host is used
    // the function might throws an exception if the provided the metadata url is not
    // a valid uri
    public String evalIdpKeyIdentifier(String idpMetadataUrl) throws URISyntaxException {
        SpidRegistration reg = identityProviders
            .stream()
            .filter(idp -> idp.getMetadataUrl().equals(idpMetadataUrl))
            .findFirst().orElse(null);;

        if (reg != null) {
            return reg.getEntityId();
        }
        return new URI(idpMetadataUrl).getHost();
    }

    // obtain an allegedly unique identifier from an idp key; this identifier can be used
    // to identify a relying party registration
    public String evalRelyingPartyRegistrationId(String idpKeyIdentifier) {
        // NOTE: this function is 'inverted' by getProviderId(..)
        return getProvider() + "|" + idpKeyIdentifier;
    }

    public String getConsumerUrl() {
        return "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "sso/" + getMetadataRegistrationId();
    }

    public String getLogoutUrl() {
        return "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "slo/" + getMetadataRegistrationId();
    }

    private String getMetadataRegistrationId() {
        return encodeRegistrationId(getProvider());
    }

    public static String encodeRegistrationId(String regId) {
        return Base64.getUrlEncoder().encodeToString(regId.getBytes());
    }

    public static String decodeRegistrationId(String encodedRegId) {
        return new String(Base64.getUrlDecoder().decode(encodedRegId), StandardCharsets.UTF_8);
    }

    /*
     * This method is invoked directly by the SpidMetadataResolver to retrieve the strictly validated
     * key pair used to physically sign the metadata XML document (<ds:Signature>).
     * Since this cryptographic operation happens internally during the metadata generation process,
     * it completely bypasses the standard Spring Security HTTP filter chains.
     * Therefore, there is NO NEED to implement a dedicated RelyingPartyRegistrationRepository
     * for the METADATA_SIGNATURE purpose. The resolved registration is safely managed and cached
     * locally within this configuration class via 'metadataSignatureRelyingPartyRegistration'.
     */
    @JsonIgnore
    public List<Credential> getMetadataRelyingPartySigningCredentials() {
        List<Credential> credentials = new ArrayList<>();
        try {
            List<SigningCredential> metadataSignatureCredentialList = SigningCredentialHelper.signingCredentialList(configMap, SigningCredentialHelper.CredentialPurpose.METADATA_SIGNATURE);

            if (metadataSignatureCredentialList.size() == 1) {
                Saml2X509Credential x509Credential = SigningCredentialHelper.createSaml2X509Credential(
                        metadataSignatureCredentialList.get(0).getSigningKey(),
                        metadataSignatureCredentialList.get(0).getSigningCertificate());

                X509Certificate certificate = x509Credential.getCertificate();
                PrivateKey privateKey = x509Credential.getPrivateKey();
                BasicCredential credential = CredentialSupport.getSimpleCredential(certificate, privateKey);
                credential.setEntityId(getEntityId());
                credential.setUsageType(UsageType.SIGNING);
                credentials.add(credential);
            }
        } catch (IOException | CertificateException e) {
            throw new RuntimeException("error building registration: " + e.getMessage());
        }
        return credentials;
    }

    // additional properties not supported by stock model
    public Boolean getRelyingPartyRegistrationIsForceAuthn() {
        // According to specs, ForceAuthn cannot be chosen for SpidL2 or SpidL3

        // return always true due to check in spid validator
        return true;
    }

    public String getEntityId() {
        return metadataConfiguration.getEntityId() != null
            ? metadataConfiguration.getEntityId()
            : getMetadataUrl();
    }

    public Set<SpidAttributeConsumingService> getAttributeConsumingServices() {
        return metadataConfiguration.getAttributeConsumingServices() != null
            ? metadataConfiguration.getAttributeConsumingServices()
            : Collections.emptySet();
    }

    public String getOrganizationName() {
        return metadataConfiguration.getOrganizationName();
    }

    public String getOrganizationDisplayName() {
        return metadataConfiguration.getOrganizationDisplayName();
    }

    public String getOrganizationUrl() {
        return metadataConfiguration.getOrganizationUrl();
    }

    public String getContactPersonEmailAddress() {
        return metadataConfiguration.getContactPersonEmailAddress();
    }

    public String getContactPersonIPACode() {
        return metadataConfiguration.getContactPersonIPACode();
    }

    public Boolean getUseAssertionConsumerServiceUrl() {
        return configMap.getUseAssertionConsumerServiceUrl() != null
            && configMap.getUseAssertionConsumerServiceUrl();
    }

    public Integer getAttributeConsumingServiceIndex() {
        return configMap.getAttributeConsumingServiceIndex() != null
            ? configMap.getAttributeConsumingServiceIndex()
            : DEFAULT_ATTRIBUTE_CONSUMING_SERVICE_INDEX;
    }

    public Set<String> getRelyingPartyRegistrationAuthnContextClassRefs() {
        return configMap.getAuthnContext() == null
            ? Collections.emptySet()
            : Collections.singleton(configMap.getAuthnContext().getValue());
    }

    public SpidPurpose getPurpose() {
        return configMap.getPurpose();
    }

    public SpidUserAttribute getSubAttributeName() {
        return configMap.getSubAttributeName();
    }

    public SpidUserAttribute getUsernameAttributeName() {
        return configMap.getUsernameAttributeName();
    }

    public Set<String> getRelyingPartyRegistrationIds() {
        Set<String> ids = new HashSet<>();

        identityProviders.stream().forEach(
            idp -> ids.add(encodeRegistrationId(evalRelyingPartyRegistrationId(idp.getEntityId())))
        );
        ids.add(getMetadataRegistrationId());
        return ids;
    }

    private String getAssertingPartyMetadataUrl(String idpEntityId) {
        SpidRegistration reg = identityProviders
            .stream()
            .filter(idp -> idp.getEntityId().equals(idpEntityId))
            .findFirst().orElse(null);
        
        if (reg != null) {
            return reg.getMetadataUrl();
        }
        return null;
    }

    private void loadMetadataConfiguration() {
        if (StringUtils.hasText(configMap.getMetadataUrl())) {
            try {
                this.metadataConfiguration = getMetadataConfigurationFromUrl(configMap.getMetadataUrl());
            } catch (IOException e) {
                throw new IllegalArgumentException("failed to download metadata from URL: " + e.getMessage(), e);
            }
        } else if (StringUtils.hasText(configMap.getMetadataXML())) {
            this.metadataConfiguration = getMetadataConfigurationFromXML(configMap.getMetadataXML());
        } else {
            Set<SpidAttributeConsumingService> attributeConsumingServices = new HashSet<>();
            if (configMap.getSpidAttributes() != null) {
                attributeConsumingServices.add(new SpidAttributeConsumingService(
                    DEFAULT_ATTRIBUTE_CONSUMING_SERVICE_INDEX,
                    DEFAULT_ATTRIBUTE_CONSUMING_SERVICE_NAME,
                    configMap.getSpidAttributes()
                ));
            }
            this.metadataConfiguration = new SpidMetadataConfiguration(
                configMap.getEntityId(),
                attributeConsumingServices,
                configMap.getOrganizationName(),
                configMap.getOrganizationDisplayName(),
                configMap.getOrganizationUrl(),
                configMap.getContactPersonEmailAddress(),
                configMap.getContactPersonIPACode()
            );
        }

        if (this.metadataConfiguration == null) {
            throw new IllegalArgumentException("empty metadata configuration");
        }
    }

    private SpidMetadataConfiguration getMetadataConfigurationFromUrl(String url) throws IOException {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("metadata url is blank");
        }
        ResponseEntity<String> response = getRestTemplate().exchange(url, HttpMethod.GET, null, String.class);
        if (response == null || response.getStatusCode() == null) {
            throw new IOException("failed to fetch metadata, empty response");
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("failed to fetch metadata, status: " + response.getStatusCode());
        }
        String xml = response.getBody();
        if (!StringUtils.hasText(xml)) {
            throw new IOException("empty metadata response body");
        }
        return getMetadataConfigurationFromXML(xml);
    }

    private SpidMetadataConfiguration getMetadataConfigurationFromXML(String xml) {
        SpidMetadataConfiguration metadataConfiguration = new SpidMetadataConfiguration();
        try {
            Document doc = parseMetadataXml(xml);
            XPath xpath = newXPath();
            String entityId = (String) xpath.evaluate(XPATH_EXPRESSION_ENTITY_ID, doc, XPathConstants.STRING);
            String organizationName = (String) xpath.evaluate(XPATH_EXPRESSION_ORGANIZATION_NAME, doc, XPathConstants.STRING);
            String organizationDisplayName = (String) xpath.evaluate(XPATH_EXPRESSION_ORGANIZATION_DISPLAY_NAME, doc, XPathConstants.STRING);
            String organizationURL = (String) xpath.evaluate(XPATH_EXPRESSION_ORGANIZATION_URL, doc, XPathConstants.STRING);
            String contactPersonEmailAddress = (String) xpath.evaluate(XPATH_EXPRESSION_CONTACT_PERSON_EMAIL_ADDRESS, doc, XPathConstants.STRING);
            String contactPersonIPACode = (String) xpath.evaluate(XPATH_EXPRESSION_CONTACT_PERSON_IPA_CODE, doc, XPathConstants.STRING);
            
            Set<SpidAttributeConsumingService> attributeConsumingServices = Collections.emptySet();
            NodeList acsList = (NodeList) xpath.evaluate(XPATH_EXPRESSION_ATTRIBUTE_CONSUMING_SERVICES, doc, XPathConstants.NODESET);
            if (acsList != null && acsList.getLength() > 0) {
                attributeConsumingServices = parseAttributeConsumingServicesFromMetadata(acsList, xpath);
            }

            metadataConfiguration.setEntityId(entityId);
            metadataConfiguration.setAttributeConsumingServices(attributeConsumingServices);
            metadataConfiguration.setOrganizationName(organizationName);
            metadataConfiguration.setOrganizationDisplayName(organizationDisplayName);
            metadataConfiguration.setOrganizationUrl(organizationURL);
            metadataConfiguration.setContactPersonEmailAddress(contactPersonEmailAddress);
            metadataConfiguration.setContactPersonIPACode(contactPersonIPACode);

            // verify metadata X509Certificate against configured signingCertificate
            verifyX509Certificate(doc, xpath);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid metadata XML: " + e.getMessage(), e);
        }

        return metadataConfiguration;
    }

    private Set<SpidAttributeConsumingService> parseAttributeConsumingServicesFromMetadata(NodeList acsList, XPath xpath) throws XPathExpressionException {
        Set<SpidAttributeConsumingService> attributeConsumingServices = new HashSet<>();

        for (int i = 0; i < acsList.getLength(); i++) {
            Element acs = (Element) acsList.item(i);
            
            String index = acs.getAttribute("index");
            String serviceName = (String) xpath.evaluate(XPATH_EXPRESSION_ACS_SERVICE_NAME, acs, XPathConstants.STRING);

            Set<SpidAttribute> attributes = new HashSet<>();
            NodeList attrList = (NodeList) xpath.evaluate(XPATH_EXPRESSION_ACS_REQUESTED_ATTRIBUTE, acs, XPathConstants.NODESET);
            for (int j = 0; j < attrList.getLength(); j++) {
                Element attr = (Element) attrList.item(j);

                String name = attr.getAttribute("Name");
                SpidAttribute attribute = SpidAttribute.parse(name);
                if (attribute != null) {
                    attributes.add(attribute);
                }
            }

            SpidAttributeConsumingService attributeConsumingService = new SpidAttributeConsumingService(Integer.parseInt(index), serviceName, attributes);
            attributeConsumingServices.add(attributeConsumingService);
        }

        Integer index = getAttributeConsumingServiceIndex();
        Boolean indexExists = attributeConsumingServices.stream().anyMatch(acs -> index.equals(acs.getIndex()));
        if (!indexExists) {
            throw new IllegalArgumentException("configured attribute consuming service index not present in metadata attribute consuming services");
        }

        return attributeConsumingServices;
    }

    private void verifyX509Certificate(Document doc, XPath xpath) throws XPathExpressionException, CertificateException, IOException {
        NodeList metaCertList = (NodeList) xpath.evaluate(XPATH_EXPRESSION_SIGNING_CERTIFICATES, doc, XPathConstants.NODESET);
        if (metaCertList == null || metaCertList.getLength() == 0) {
            throw new IllegalArgumentException("empty signing certificate in metadata");
        }

        Set<String> metaCertNormalizedList = new HashSet<>();
        for (int i = 0; i < metaCertList.getLength(); i++) {
            String metaCert = metaCertList.item(i).getTextContent();
            String metaCertNormalized = stripPem(metaCert);
            if (StringUtils.hasText(metaCertNormalized)) {
                metaCertNormalizedList.add(metaCertNormalized);
            }
        }
        if (metaCertNormalizedList.isEmpty()) {
            throw new IllegalArgumentException("no valid signing certificates found in metadata");
        }

        List<SigningCredential> signingCredentialList = SigningCredentialHelper.signingCredentialList(configMap, SigningCredentialHelper.CredentialPurpose.METADATA_EXPOSURE);

        Set<String> confCertNormalizedList = signingCredentialList
            .stream()
            .map(SigningCredential::getSigningCertificate)
            .filter(StringUtils::hasText)
            .map(c -> stripPem(c))
            .collect(Collectors.toSet());

        // Check set equality via double inclusion:

        // 1. Check if all certificates in metadata are present in configuration
        if (!confCertNormalizedList.containsAll(metaCertNormalizedList)) {
            Set<String> extraInMeta = new HashSet<>(metaCertNormalizedList);
            extraInMeta.removeAll(confCertNormalizedList);
            throw new IllegalArgumentException(
                    "Signing certificate discrepancy: metadata contains certificates not found in local configuration. " +
                            "Extra certificates found in metadata: " + extraInMeta
            );
        }

        // 2. Check if all configured certificates are present in metadata
        if (!metaCertNormalizedList.containsAll(confCertNormalizedList)) {
            Set<String> missingInMeta = new HashSet<>(confCertNormalizedList);
            missingInMeta.removeAll(metaCertNormalizedList);
            throw new IllegalArgumentException(
                    "Signing certificate discrepancy: configured certificates are missing from metadata. " +
                            "Missing certificates from metadata: " + missingInMeta
            );
        }
    }

    private Document parseMetadataXml(String xml) throws ParserConfigurationException, IOException, SAXException {
        if (!StringUtils.hasText(xml)) {
            throw new IllegalArgumentException("no metadata XML configured");
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return db.parse(bais);
        }
    }

    private XPath newXPath() {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                switch (prefix) {
                    case "md":
                        return "urn:oasis:names:tc:SAML:2.0:metadata";
                    case "ds":
                        return "http://www.w3.org/2000/09/xmldsig#";
                    case "spid":
                        return "https://spid.gov.it/saml-extensions";
                    case "xml":
                        return "http://www.w3.org/XML/1998/namespace";
                    default:
                        return javax.xml.XMLConstants.NULL_NS_URI;
                }
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return null;
            }
        });
        return xpath;
    }

    private String stripPem(String cert) {
        if (cert == null) return null;
        return cert
            .replaceAll("-----BEGIN CERTIFICATE-----", "")
            .replaceAll("-----END CERTIFICATE-----", "")
            .replaceAll("\\s+", "");
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            RequestConfig rconfig = RequestConfig
                .custom()
                .setConnectTimeout(DEFAULT_TIMEOUT)
                .setConnectionRequestTimeout(DEFAULT_TIMEOUT)
                .setSocketTimeout(DEFAULT_TIMEOUT)
                .build();
            CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(rconfig).build();
            HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(client);
            restTemplate = new RestTemplate(clientHttpRequestFactory);
        }
        return restTemplate;
    }
}
