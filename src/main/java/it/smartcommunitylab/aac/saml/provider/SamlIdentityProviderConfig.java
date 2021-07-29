package it.smartcommunitylab.aac.saml.provider;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;

public class SamlIdentityProviderConfig extends AbstractConfigurableProvider {

    public static final String DEFAULT_METADATA_URL = "{baseUrl}" + SamlIdentityAuthority.AUTHORITY_URL
            + "metadata/{registrationId}";

    public static final String DEFAULT_CONSUMER_URL = "{baseUrl}" + SamlIdentityAuthority.AUTHORITY_URL
            + "sso/{registrationId}";

    private String name;
    private String description;
    private String persistence;
    private Boolean linkable;

    private SamlIdentityProviderConfigMap configMap;
    private RelyingPartyRegistration relyingPartyRegistration;

    // hook functions
    private Map<String, String> hookFunctions;

    public SamlIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm);
        this.relyingPartyRegistration = null;
        this.configMap = new SamlIdentityProviderConfigMap();
        // set default params, will set vars after build
        this.configMap.setEntityId(DEFAULT_METADATA_URL);
        this.configMap.setMetadataUrl(DEFAULT_METADATA_URL);
        this.configMap.setAssertionConsumerServiceUrl(DEFAULT_CONSUMER_URL);

        this.hookFunctions = Collections.emptyMap();

    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SamlIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(SamlIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public Boolean getLinkable() {
        return linkable;
    }

    public void setLinkable(Boolean linkable) {
        this.linkable = linkable;
    }

    public boolean isLinkable() {
        if (linkable != null) {
            return linkable.booleanValue();
        }

        return true;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new SamlIdentityProviderConfigMap();
        configMap.setConfiguration(props);

        configMap.setMetadataUrl(DEFAULT_METADATA_URL);
        configMap.setAssertionConsumerServiceUrl(DEFAULT_CONSUMER_URL);

    }

    public RelyingPartyRegistration getRelyingPartyRegistration() {
        if (relyingPartyRegistration == null) {
            try {
                relyingPartyRegistration = toRelyingPartyRegistration();
            } catch (IOException | CertificateException e) {
                throw new RuntimeException("error building registration: " + e.getMessage());
            }
        }

        return relyingPartyRegistration;
    }

    // TODO throws exception if configuration is invalid
    private RelyingPartyRegistration toRelyingPartyRegistration() throws IOException, CertificateException {
        // set base parameters
        String entityId = DEFAULT_METADATA_URL;
        String assertionConsumerServiceLocation = DEFAULT_CONSUMER_URL;

        if (StringUtils.hasText(configMap.getEntityId())) {
            // let config override, this breaks some standards but saml...
            entityId = configMap.getEntityId();
        }

        // read rp parameters from map
        // note: only RSA keys supported
        String signingKey = configMap.getSigningKey();
        String signingCertificate = configMap.getSigningCertificate();
        String cryptKey = configMap.getCryptKey();
        String cryptCertificate = configMap.getCryptCertificate();

        // ap autoconfiguration
        String idpMetadataLocation = configMap.getIdpMetadataUrl();
        // ap manual configuration (only if not metadata)
        String assertingPartyEntityId = configMap.getIdpEntityId();
        String ssoLoginServiceLocation = configMap.getWebSsoUrl();
        String ssoLogoutServiceLocation = configMap.getWebLogoutUrl();
        boolean signAuthNRequest = (configMap.getSignAuthNRequest() != null
                ? configMap.getSignAuthNRequest().booleanValue()
                : true);
        String verificationCertificate = configMap.getVerificationCertificate();
        Saml2MessageBinding ssoServiceBinding = getServiceBinding(configMap.getSsoServiceBinding());

        // via builder
        // providerId is unique, use as registrationId
        String registrationId = getProvider();
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId(registrationId);

        if (StringUtils.hasText(idpMetadataLocation)) {
            // read metadata to autoconfigure
            builder = RelyingPartyRegistrations
                    .fromMetadataLocation(idpMetadataLocation)
                    .registrationId(registrationId);
        } else {
            // set manually
            builder.assertingPartyDetails((party) -> party
                    .entityId(assertingPartyEntityId)
                    .singleSignOnServiceLocation(ssoLoginServiceLocation)
                    .wantAuthnRequestsSigned(signAuthNRequest)
                    .singleSignOnServiceBinding(ssoServiceBinding));

            if (StringUtils.hasText(verificationCertificate)) {
                Saml2X509Credential verificationCredentials = getVerificationCertificate(verificationCertificate);
                builder.assertingPartyDetails((party) -> party
                        .verificationX509Credentials((c) -> c.add(verificationCredentials)));
            }

        }

        // set fixed config params
        builder
                .entityId(entityId)
                .assertionConsumerServiceLocation(assertionConsumerServiceLocation);

        // check if sign credentials are provided
        if (StringUtils.hasText(signingKey) && StringUtils.hasText(signingCertificate)) {
//            // cleanup pem
//            signingCertificate = cleanupPem("CERTIFICATE", signingCertificate);

            Saml2X509Credential signingCredentials = getCredentials(signingKey, signingCertificate,
                    Saml2X509CredentialType.SIGNING, Saml2X509CredentialType.DECRYPTION);
            // add for signature
            builder.signingX509Credentials((c) -> c.add(signingCredentials));

            // we use these also for decrypt
            builder.decryptionX509Credentials((c) -> c.add(signingCredentials));
        }

        if (StringUtils.hasText(cryptKey) && StringUtils.hasText(cryptCertificate)) {
            // cleanup spaces, base64 encoding certs are expected
//            cryptKey = cleanupPem("PRIVATE KEY", cryptKey);
//            cryptCertificate = cleanupPem("CERTIFICATE", cryptCertificate);

            Saml2X509Credential cryptCredentials = getCredentials(cryptKey, cryptCertificate,
                    Saml2X509CredentialType.ENCRYPTION, Saml2X509CredentialType.DECRYPTION);
            // add to decrypt credentials
            builder.decryptionX509Credentials(c -> c.add(cryptCredentials));
            // also use to encrypt messages
            builder.assertingPartyDetails((party) -> party
                    .encryptionX509Credentials((c) -> c.add(cryptCredentials)));
        }

        return builder.build();

    }

    // export additional properties not supported by stock model
    public Boolean getRelyingPartyRegistrationIsForceAuthn() {
        return configMap.getForceAuthn();
    }

    public Boolean getRelyingPartyRegistrationIsPassive() {
        return configMap.getIsPassive();
    }

    public String getRelyingPartyRegistrationNameIdFormat() {
        return configMap.getNameIDFormat();
    }

    public Boolean getRelyingPartyRegistrationNameIdAllowCreate() {
        return configMap.getNameIDAllowCreate();
    }

    public Set<String> getRelyingPartyRegistrationAuthnContextClassRefs() {
        return configMap.getAuthnContextClasses();
    }

    public String getRelyingPartyRegistrationAuthnContextComparison() {
        return configMap.getAuthnContextComparison();
    }

    //
    private Saml2X509Credential getVerificationCertificate(String certificate)
            throws CertificateException, IOException {
        return new Saml2X509Credential(
                parseX509Certificate(certificate),
                Saml2X509CredentialType.VERIFICATION);
    }

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

//    // TODO rewrite with proper parser
//    private String cleanupPem(String kind, String value) {
//        // build headers
//        // we set a fixed length separator because spring rsaKeyConverter checks for
//        // this specific amount of dashes..
//        String sep = "-----";
//        String header = "BEGIN " + kind;
//        String footer = "END " + kind;
//
//        String[] lines = value.split("\\R");
//        String[] keyLines = lines;
//
//        if (lines.length > 2) {
//            // headers?
//            String headerLine = lines[0];
//            String footerLine = lines[lines.length - 1];
//
//            if (headerLine.contains(header)) {
//                // extract key
//                keyLines = new String[lines.length - 2];
//                System.arraycopy(lines, 1, keyLines, 0, keyLines.length);
//            }
//        }
//
//        // cleanup and rebuild string
//        StringBuilder sb = new StringBuilder();
//        sb.append(sep).append(header).append(sep).append("\n");
//        for (int c = 0; c < keyLines.length; c++) {
//            sb.append(keyLines[c].trim()).append("\n");
//        }
//
//        sb.append(sep).append(footer).append(sep);
//        return sb.toString();
//    }

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
    public static ConfigurableProvider toConfigurableProvider(SamlIdentityProviderConfig sp) {
        ConfigurableProvider cp = new ConfigurableProvider(SystemKeys.AUTHORITY_SAML, sp.getProvider(), sp.getRealm());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);
        cp.setPersistence(sp.getPersistence());

        cp.setName(sp.getName());
        cp.setDescription(sp.getDescription());
        cp.setHookFunctions(sp.getHookFunctions());

        cp.setEnabled(true);
        cp.setLinkable(sp.isLinkable());
        cp.setConfiguration(sp.getConfiguration());
        return cp;
    }

    public static SamlIdentityProviderConfig fromConfigurableProvider(ConfigurableProvider cp) {
        SamlIdentityProviderConfig sp = new SamlIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        sp.setConfiguration(cp.getConfiguration());

        sp.name = cp.getName();
        sp.description = cp.getDescription();
        sp.persistence = cp.getPersistence();
        sp.linkable = cp.isLinkable();
        sp.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return sp;

    }

}
