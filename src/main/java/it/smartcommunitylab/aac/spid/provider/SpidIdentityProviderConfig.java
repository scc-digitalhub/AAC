package it.smartcommunitylab.aac.spid.provider;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.UsageType;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;

public class SpidIdentityProviderConfig extends AbstractIdentityProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    public static final String DEFAULT_METADATA_URL = "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL
            + "metadata/{registrationId}";

    public static final String DEFAULT_CONSUMER_URL = "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL
            + "sso/{registrationId}";

    public static final String DEFAULT_LOGOUT_URL = "{baseUrl}" + SpidIdentityAuthority.AUTHORITY_URL
            + "slo/{registrationId}";

    private SpidIdentityProviderConfigMap configMap;
    private Set<RelyingPartyRegistration> relyingPartyRegistrations;
    private Map<String, SpidRegistration> idps;

    public SpidIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_SPID, provider, realm);
        this.relyingPartyRegistrations = null;
        this.configMap = new SpidIdentityProviderConfigMap();
        // set default params, will set vars after build
        this.configMap.setEntityId(DEFAULT_METADATA_URL);
        this.configMap.setMetadataUrl(DEFAULT_METADATA_URL);
        this.configMap.setAssertionConsumerServiceUrl(DEFAULT_CONSUMER_URL);

        this.idps = Collections.emptyMap();
    }

    public void setIdps(Collection<SpidRegistration> idps) {
        if (idps != null) {
            this.idps = idps.stream().collect(Collectors.toMap(e -> e.getEntityId(), e -> e));
        }
    }

    public SpidIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(SpidIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    @Override
    public String getPersistence() {
        return SystemKeys.PERSISTENCE_LEVEL_SESSION;
    }

    @Override
    public void setPersistence(String persistence) {
        this.persistence = SystemKeys.PERSISTENCE_LEVEL_SESSION;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new SpidIdentityProviderConfigMap();
        configMap.setConfiguration(props);

        configMap.setMetadataUrl(DEFAULT_METADATA_URL);
        configMap.setAssertionConsumerServiceUrl(DEFAULT_CONSUMER_URL);
        configMap.setSingleLogoutUrl(DEFAULT_LOGOUT_URL);
    }

    public Set<String> getRelyingPartyMetadataUrls() {
        Set<String> idpMetadataUrls = new HashSet<>();

        // urls have priority
        if (StringUtils.hasText(configMap.getIdpMetadataUrl())) {
            // single idp via url
            idpMetadataUrls = Collections.singleton(configMap.getIdpMetadataUrl());
        } else if (configMap.getIdps() != null && !configMap.getIdps().isEmpty()) {
            // resolve entityId to metadata
            if (idps != null) {
                for (String idp : configMap.getIdps()) {
                    SpidRegistration idpReg = idps.get(idp);
                    if (idpReg != null) {
                        idpMetadataUrls.add(idpReg.getMetadataUrl());
                    }
                }
            }
        } else {
            // register all in map
            if (idps != null) {
                for (SpidRegistration idpReg : idps.values()) {
                    idpMetadataUrls.add(idpReg.getMetadataUrl());
                }
            }
        }
        if (idpMetadataUrls == null || idpMetadataUrls.isEmpty()) {
            throw new IllegalArgumentException("invalid configuration");
        }

        return idpMetadataUrls;
    }

    public Set<String> getRelyingPartyRegistrationIds() {
        Set<String> idpMetadataUrls = getRelyingPartyMetadataUrls();
        return idpMetadataUrls.stream().map(u -> {
            try {
                return getRelyingPartyRegistrationId(getIdpKey(u));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("invalid metadata uri " + e.getMessage());
            }
        }).collect(Collectors.toSet());
    }

    public Collection<SpidRegistration> getSpidRegistrations() {
        Set<String> idpMetadataUrls = getRelyingPartyMetadataUrls();
        return idpMetadataUrls.stream().map(u -> {
            try {
                return getIdp(u);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("invalid metadata uri " + e.getMessage());
            }
        }).collect(Collectors.toSet());
    }

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

    private Set<RelyingPartyRegistration> toRelyingPartyRegistrations() throws IOException, CertificateException {
        Set<RelyingPartyRegistration> registrations = new HashSet<>();
        try {
            Set<String> idpMetadataUrls = getRelyingPartyMetadataUrls();
            for (String idpMetadataUrl : idpMetadataUrls) {
                registrations.add(toRelyingPartyRegistration(idpMetadataUrl));
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid metadata uri " + e.getMessage());
        }

        if (registrations.isEmpty()) {
            throw new IllegalArgumentException("invalid configuration");
        }

        // add a global registration for metadata
        RelyingPartyRegistration meta = RelyingPartyRegistration
                .withRelyingPartyRegistration(registrations.iterator().next())
                .registrationId(getProvider()).build();
        registrations.add(meta);

        return registrations;
    }

    // TODO throws exception if configuration is invalid
    private RelyingPartyRegistration toRelyingPartyRegistration(String idpMetadataUrl)
            throws IOException, CertificateException, URISyntaxException {
        // set base parameters
        String entityId = DEFAULT_METADATA_URL;
        String assertionConsumerServiceLocation = DEFAULT_CONSUMER_URL;
        String singleLogoutServiceLocation = DEFAULT_LOGOUT_URL;

        if (StringUtils.hasText(configMap.getEntityId())) {
            // let config override, this breaks some standards but saml...
            entityId = configMap.getEntityId();
        }

        // read rp parameters from map
        // note: only RSA keys supported
        String signingKey = configMap.getSigningKey();
        String signingCertificate = configMap.getSigningCertificate();
//        String cryptKey = configMap.getCryptKey();
//        String cryptCertificate = configMap.getCryptCertificate();

        // ap autoconfiguration
        String idpMetadataLocation = idpMetadataUrl;
        boolean signAuthNRequest = (configMap.getSignAuthNRequest() != null
                ? configMap.getSignAuthNRequest().booleanValue()
                : true);
        Saml2MessageBinding ssoServiceBinding = getServiceBinding(configMap.getSsoServiceBinding());

        // via builder
        // providerId is unique, use as registrationId
        // extract name from url
        String idpKey = getIdpKey(idpMetadataUrl);
        String registrationId = getRelyingPartyRegistrationId(idpKey);
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId(registrationId);

        // read metadata to autoconfigure
        builder = RelyingPartyRegistrations
                .fromMetadataLocation(idpMetadataLocation)
                .registrationId(registrationId);

        // set fixed config params
        builder
                .entityId(entityId)
                .assertionConsumerServiceLocation(assertionConsumerServiceLocation);

        // check if sign credentials are provided
        if (StringUtils.hasText(signingKey) && StringUtils.hasText(signingCertificate)) {

            Saml2X509Credential signingCredentials = getCredentials(signingKey, signingCertificate,
                    Saml2X509CredentialType.SIGNING, Saml2X509CredentialType.DECRYPTION);
            // add for signature
            builder.signingX509Credentials((c) -> c.add(signingCredentials));

            // we use these also for decrypt
            builder.decryptionX509Credentials((c) -> c.add(signingCredentials));
        }

        // disable encryption
//        if (StringUtils.hasText(cryptKey) && StringUtils.hasText(cryptCertificate)) {
//
//            Saml2X509Credential cryptCredentials = getCredentials(cryptKey, cryptCertificate,
//                    Saml2X509CredentialType.ENCRYPTION, Saml2X509CredentialType.DECRYPTION);
//            // add to decrypt credentials
//            builder.decryptionX509Credentials(c -> c.add(cryptCredentials));
//            // also use to encrypt messages
//            builder.assertingPartyDetails((party) -> party
//                    .encryptionX509Credentials((c) -> c.add(cryptCredentials)));
//        }

        return builder.build();

    }

    public SpidRegistration getIdp(String idpMetadataUrl) throws URISyntaxException {
        // check if registration
        Optional<SpidRegistration> reg = idps.values().stream().filter(r -> r.getMetadataUrl().equals(idpMetadataUrl))
                .findFirst();
        if (reg.isPresent()) {
            return reg.get();
        }

        // build generic
        SpidRegistration sreg = new SpidRegistration();
        sreg.setEntityId(idpMetadataUrl);
        sreg.setMetadataUrl(idpMetadataUrl);

        // extract name from url
        URI uri = new URI(idpMetadataUrl);
        String idpName = uri.getHost();
        sreg.setEntityName(idpName);
        sreg.setEntityLabel(idpName);

        return sreg;
    }

    public String getIdpKey(String idpMetadataUrl) throws URISyntaxException {
        // check if registration
        Optional<SpidRegistration> reg = idps.values().stream().filter(r -> r.getMetadataUrl().equals(idpMetadataUrl))
                .findFirst();
        if (reg.isPresent()) {
            return reg.get().getEntityLabel();
        }

        // extract name from url
        URI uri = new URI(idpMetadataUrl);
        return uri.getHost();
    }

    public String getIdpIcon(String idpMetadataUrl) throws URISyntaxException {
        // check if registration
        Optional<SpidRegistration> reg = idps.values().stream().filter(r -> r.getMetadataUrl().equals(idpMetadataUrl))
                .findFirst();
        if (reg.isPresent()) {
            return "spid/img/spid-idp-" + reg.get().getEntityLabel() + ".svg";
        }

        // generic icon
        return "spid/img/spid-ico-circle-bb.svg";
    }

    public String getRelyingPartyRegistrationId(String idpKey) {
        return getProvider() + "-" + idpKey;
    }

//    // export additional properties not supported by stock model
    public Boolean getRelyingPartyRegistrationIsForceAuthn() {
//        if (SpidAuthnContext.SPID_L2 == configMap.getAuthnContext()
//                || SpidAuthnContext.SPID_L3 == configMap.getAuthnContext()) {
//            return true;
//        }
//        ;
//        // as per spec, for L1 we do NOT include ForceAuthn at all
//        return null;

        // return always true due to check in spid validator
        return true;
    }

    public String getRelyingPartyRegistrationSingleLogoutConsumerServiceLocation() {
        return DEFAULT_LOGOUT_URL;
    }

//
//    public Boolean getRelyingPartyRegistrationIsPassive() {
//        return configMap.getIsPassive();
//    }
//
//    public String getRelyingPartyRegistrationNameIdFormat() {
//        return configMap.getNameIDFormat();
//    }
//
//    public Boolean getRelyingPartyRegistrationNameIdAllowCreate() {
//        return configMap.getNameIDAllowCreate();
//    }
//
    public Set<String> getRelyingPartyRegistrationAuthnContextClassRefs() {
        return configMap.getAuthnContext() == null ? Collections.emptySet()
                : Collections.singleton(configMap.getAuthnContext().getValue());
    }

    public List<Credential> getRelyingPartySigningCredentials() {
        List<Credential> credentials = new ArrayList<>();
        RelyingPartyRegistration rp = getRelyingPartyRegistrations().stream().findFirst().orElse(null);
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

    public SpidUserAttribute getIdAttribute() {
        return configMap.getIdAttribute();
    }

//
//    public String getRelyingPartyRegistrationAuthnContextComparison() {
//        return configMap.getAuthnContextComparison();
//    }

    private Saml2X509Credential getCredentials(String key, String certificate, Saml2X509CredentialType... keyUse)
            throws IOException, CertificateException {
//        PrivateKey pk = RsaKeyConverters.pkcs8().convert(new ByteArrayInputStream(key.getBytes()));
        PrivateKey pk = parsePrivateKeyFallback(key);
        X509Certificate cert = parseX509Certificate(certificate);
        return new Saml2X509Credential(pk, cert, keyUse);
    }

    private PrivateKey parsePrivateKeyFallback(String key) throws IOException {
        // first try as rsa
        PrivateKey pk = null;
        try {
            pk = parsePrivateKey(fixPem(key, "RSA PRIVATE KEY"));
        } catch (IllegalArgumentException | IOException e) {
            // fallback as private
            pk = parsePrivateKey(fixPem(key, "PRIVATE KEY"));
        }

        return pk;
    }

    private PrivateKey parsePrivateKey(String key) throws IOException {
        StringReader sr = new StringReader(key);
        PEMParser pr = new PEMParser(sr);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        Object pem = pr.readObject();
        sr.close();

        if (pem instanceof PEMKeyPair) {
            PrivateKeyInfo privateKeyInfo = ((PEMKeyPair) pem).getPrivateKeyInfo();
            return converter.getPrivateKey(privateKeyInfo);
        } else if (pem instanceof PrivateKeyInfo) {
            return converter.getPrivateKey((PrivateKeyInfo) pem);
        }

        throw new IllegalArgumentException("invalid private key");
    }

//    private X509Certificate parseX509Certificate(String source) {
//        try {
//            final CertificateFactory factory = CertificateFactory.getInstance("X.509");
//            return (X509Certificate) factory.generateCertificate(
//                    new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
//        } catch (Exception e) {
//            throw new IllegalArgumentException(e);
//        }
//    }

    private X509Certificate parseX509Certificate(String source) throws IOException, CertificateException {
        String src = fixPem(source, "CERTIFICATE");
        StringReader sr = new StringReader(src);
        PEMParser pr = new PEMParser(sr);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        Object pem = pr.readObject();
        sr.close();

        if (pem instanceof X509CertificateHolder) {
            return converter.getCertificate((X509CertificateHolder) pem);
        }

        throw new IllegalArgumentException("invalid certificate");
    }

    private Saml2MessageBinding getServiceBinding(String value) {
        Saml2MessageBinding ssoServiceBinding = Saml2MessageBinding.POST;
        if ("HTTP-Redirect".equals(value)) {
            ssoServiceBinding = Saml2MessageBinding.REDIRECT;
        }

        return ssoServiceBinding;
    }

    private String fixPem(String value, String kind) {
        String sep = "-----";
        String begin = "BEGIN " + kind;
        String end = "END " + kind;

        String header = sep + begin + sep;
        String footer = sep + end + sep;

        String[] lines = value.split("\\R");

        if (lines.length > 2) {
            // headers?
            String headerLine = lines[0];
            String footerLine = lines[lines.length - 1];

            if (headerLine.startsWith(sep) && footerLine.startsWith(sep)) {
                // return unchanged, don't mess with content
                return value;
            }
        }

        // rewrite
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (int c = 0; c < lines.length; c++) {
            sb.append(lines[c].trim()).append("\n");
        }
        sb.append(footer);
        return sb.toString();
    }

    /*
     * builders
     */
//    public static ConfigurableIdentityProvider toConfigurableProvider(SpidIdentityProviderConfig sp) {
//        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(SystemKeys.AUTHORITY_SPID, sp.getProvider(),
//                sp.getRealm());
//        cp.setType(SystemKeys.RESOURCE_IDENTITY);
//        cp.setPersistence(sp.getPersistence());
//
//        cp.setName(sp.getName());
//        cp.setDescription(sp.getDescription());
//        cp.setHookFunctions(sp.getHookFunctions());
//
//        cp.setEnabled(true);
//        cp.setLinkable(false);
//        cp.setConfiguration(sp.getConfiguration());
//        return cp;
//    }

    public static SpidIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
        SpidIdentityProviderConfig sp = new SpidIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        sp.setConfiguration(cp.getConfiguration());

        sp.name = cp.getName();
        sp.description = cp.getDescription();
        sp.icon = cp.getIcon();

        sp.persistence = cp.getPersistence();
        sp.linkable = cp.isLinkable();
        sp.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return sp;

    }

    public static String getProviderId(String registrationId) {
        Assert.hasText(registrationId, "registrationId can not be blank");

        // registrationId is providerId+idpkey
        String[] kp = registrationId.split("-");
        if (kp.length < 2) {
            throw new IllegalArgumentException();
        }

        return kp[0];
    }

}
