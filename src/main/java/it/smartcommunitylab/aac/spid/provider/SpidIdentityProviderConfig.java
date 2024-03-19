package it.smartcommunitylab.aac.spid.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.crypto.CertificateParser;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.model.SpidIdPRegistration;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.stream.Collectors;

public class SpidIdentityProviderConfig extends AbstractIdentityProviderConfig<SpidIdentityProviderConfigMap> {

    public static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + SpidIdentityProviderConfigMap.RESOURCE_TYPE;

    public static final String DEFAULT_METADATA_URL = "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "metadata/{registrationId}";
    public static final String DEFAULT_CONSUMER_URL = "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "sso/{registrationId}";
    public static final String DEFAULT_LOGOUT_URL = "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL + "slo/{registrationId}";

    private transient Set<RelyingPartyRegistration> relyingPartyRegistrations; // first time evaluated by the getter, then immutable
    private Map<String, SpidIdPRegistration> identityProviders;

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

    public void setIdentityProviders(Collection<SpidIdPRegistration> idps) {
        if (idps != null) {
            this.identityProviders = idps.stream().collect(Collectors.toMap(SpidIdPRegistration::getEntityId, r -> r));
        }
    }

    public String getMetadataUrl() {
        return DEFAULT_METADATA_URL;
    }

    public String getEntityId() {
        return configMap.getEntityId() != null ? configMap.getEntityId() : getMetadataUrl();
    }

    public String getRepositoryIds() {
        return getProvider(); // TODO: reconsider when more of this will become clear
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

    // generate a registration (an RP/AP pair as defined by OpenSaml) for _each_
    // configured
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
            throw new IllegalArgumentException("invalid configuration: failed to acquire any relaying party registration for spid provider " + getProvider());
        }
        
        // add a global registration for metadata
        RelyingPartyRegistration meta = RelyingPartyRegistration
                .withRelyingPartyRegistration(registrations.iterator().next())
                .registrationId(getProvider()).build();
        registrations.add(meta);
        
        return registrations;
    }

    // returns the list of metadata urls of upstream idps
    private Set<String> getAssertingPartyMetadataUrls() {
        Set<String> idpMetadataUrls =new HashSet<>();

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
            for (String idp: configMap.getIdps()) {
                SpidIdPRegistration reg = identityProviders.get(idp);
                if (reg != null) {
                    idpMetadataUrls.add(reg.getMetadataUrl());
                }
            }
        } else {
            for (SpidIdPRegistration reg : identityProviders.values()) {
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
    private String evalIdpKeyIdentifier(String idpMetadataUrl) throws URISyntaxException {
        Optional<SpidIdPRegistration> reg = this.identityProviders.values().stream().filter(r -> r.getMetadataUrl().equals(idpMetadataUrl)).findFirst();
        if (reg.isPresent()) {
            return reg.get().getEntityLabel();
        }
        return new URI(idpMetadataUrl).getHost();
    }

    // obtain an allegedly unique identifier from an idp key; this identifier can be used
    // to identify a relying party registration
    private String evalRelyingPartyRegistrationId(String idpKeyIdentifier) {
//        String idpKeyIdentifier = evalIdpKeyIdentifier(idpMetadataUrl);
        return getProvider() + "-" + idpKeyIdentifier;
    }

    // create a relying party registration for an upstream idp; only ap autoconfiguration
    // is supported, hence input require an idp metadata url
    private RelyingPartyRegistration toRelyingPartyRegistration(String idpMetadataUrl) throws IOException, CertificateException, URISyntaxException {
        // start from ap autoconfiguration ...
        String key = evalIdpKeyIdentifier(idpMetadataUrl);
        String registrationId = evalRelyingPartyRegistrationId(key);
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistrations.fromMetadataLocation(idpMetadataUrl).registrationId(registrationId);

        // ... then expand with rp configuration (i.e. ourself)
        builder
                .entityId(getEntityId())
                .assertionConsumerServiceLocation(DEFAULT_CONSUMER_URL);
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
}
