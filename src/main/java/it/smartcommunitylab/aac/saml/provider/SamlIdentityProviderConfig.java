package it.smartcommunitylab.aac.saml.provider;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.springframework.security.converter.RsaKeyConverters;
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

    protected SamlIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    // TODO throws exception if configuration is invalid
    public RelyingPartyRegistration toRelyingPartyRegistration() {
        // set base parameters
        String entityId = DEFAULT_METADATA_URL;
        String assertionConsumerServiceLocation = DEFAULT_CONSUMER_URL;

        // read rp parameters from map
        // note: only RSA keys supported
        String signingKey = getConfigurationProperty("signingKey");
        String signingCertificate = getConfigurationProperty("signingCertificate");
        String cryptKey = getConfigurationProperty("cryptKey");
        String cryptCertificate = getConfigurationProperty("cryptCertificate");

        // ap autoconfiguration
        String idpMetadataLocation = getConfigurationProperty("idpMetadataUrl");
        // ap manual configuration (only if not metadata)
        String assertingPartyEntityId = getConfigurationProperty("idpEntityId");
        String ssoLoginServiceLocation = getConfigurationProperty("webSsoUrl");
        String ssoLogoutServiceLocation = getConfigurationProperty("webLogoutUrl");
        boolean signAuthNRequest = Boolean.parseBoolean(getProperty("signAuthNRequest", "true"));
        String verificationCertificate = getConfigurationProperty("verificationCertificate");
        Saml2MessageBinding ssoServiceBinding = getServiceBinding(getProperty("ssoServiceBinding", "HTTP-POST"));

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
            Saml2X509Credential signingCredentials = getCredentials(signingKey, signingCertificate,
                    Saml2X509CredentialType.SIGNING, Saml2X509CredentialType.DECRYPTION);
            // add for signature
            builder.signingX509Credentials((c) -> c.add(signingCredentials));

            // we use these also for decrypt
            builder.decryptionX509Credentials((c) -> c.add(signingCredentials));
        }

        if (StringUtils.hasText(cryptKey) && StringUtils.hasText(cryptCertificate)) {
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

    private Saml2X509Credential getVerificationCertificate(String certificate) {
        return new Saml2X509Credential(
                x509Certificate(certificate),
                Saml2X509CredentialType.VERIFICATION);
    }

    private Saml2X509Credential getCredentials(String key, String certificate, Saml2X509CredentialType... keyUse) {
        PrivateKey pk = RsaKeyConverters.pkcs8().convert(new ByteArrayInputStream(key.getBytes()));
        X509Certificate cert = x509Certificate(certificate);
        return new Saml2X509Credential(pk, cert, keyUse);
    }

    private X509Certificate x509Certificate(String source) {
        try {
            final CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Saml2MessageBinding getServiceBinding(String value) {
        Saml2MessageBinding ssoServiceBinding = Saml2MessageBinding.POST;
        if ("HTTP-Redirect".equals(value)) {
            ssoServiceBinding = Saml2MessageBinding.REDIRECT;
        }

        return ssoServiceBinding;
    }

    private String getProperty(String key, String defaultValue) {
        if (StringUtils.hasText(getConfigurationProperty(key))) {
            return getConfigurationProperty(key);
        }

        return defaultValue;
    }

    /*
     * builders
     */
    public static ConfigurableProvider toConfigurableProvider(SamlIdentityProviderConfig op) {
        ConfigurableProvider cp = new ConfigurableProvider(SystemKeys.AUTHORITY_SAML, op.getProvider(), op.getRealm());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);
        cp.setConfiguration(op.getConfiguration());
        cp.setName(op.name);
        return cp;
    }

    public static SamlIdentityProviderConfig fromConfigurableProvider(ConfigurableProvider cp) {
        SamlIdentityProviderConfig op = new SamlIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        op.setConfiguration(cp.getConfiguration());
        op.setName(cp.getName());
        return op;

    }

}
