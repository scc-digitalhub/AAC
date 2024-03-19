/*
 * Copyright 2023 the original author or authors
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

package it.smartcommunitylab.aac.saml.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.crypto.CertificateParser;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.StringUtils;

public class SamlIdentityProviderConfig extends AbstractIdentityProviderConfig<SamlIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + SamlIdentityProviderConfigMap.RESOURCE_TYPE;

    private transient RelyingPartyRegistration relyingPartyRegistration;

    public SamlIdentityProviderConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_SAML, provider, realm);
    }

    public SamlIdentityProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new IdentityProviderSettingsMap(), new SamlIdentityProviderConfigMap());
        this.relyingPartyRegistration = null;
    }

    public SamlIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        IdentityProviderSettingsMap settingsMap,
        SamlIdentityProviderConfigMap configMap
    ) {
        super(cp, settingsMap, configMap);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */

    @SuppressWarnings("unused")
    private SamlIdentityProviderConfig() {
        super();
    }

    public String getRepositoryId() {
        // not configurable, always isolate saml providers
        return getProvider();
    }

    @JsonIgnore
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
        String entityId = getEntityId();
        String assertionConsumerServiceLocation = getAssertionConsumerUrl();

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
        boolean signAuthNRequest =
            (configMap.getSignAuthNRequest() != null ? configMap.getSignAuthNRequest().booleanValue() : true);
        String verificationCertificate = configMap.getVerificationCertificate();
        Saml2MessageBinding ssoServiceBinding = getServiceBinding(configMap.getSsoServiceBinding());

        // via builder
        // providerId is unique, use as registrationId
        String registrationId = getProvider();
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId(registrationId);

        if (StringUtils.hasText(idpMetadataLocation)) {
            // read metadata to autoconfigure
            builder =
                RelyingPartyRegistrations.fromMetadataLocation(idpMetadataLocation).registrationId(registrationId);
        } else {
            // set manually
            builder.assertingPartyDetails(party ->
                party
                    .entityId(assertingPartyEntityId)
                    .singleSignOnServiceLocation(ssoLoginServiceLocation)
                    .wantAuthnRequestsSigned(signAuthNRequest)
                    .singleSignOnServiceBinding(ssoServiceBinding)
            );

            if (StringUtils.hasText(verificationCertificate)) {
                Saml2X509Credential verificationCredentials = getVerificationCertificate(verificationCertificate);
                builder.assertingPartyDetails(party ->
                    party.verificationX509Credentials(c -> c.add(verificationCredentials))
                );
            }
        }

        // set fixed config params
        builder.entityId(entityId).assertionConsumerServiceLocation(assertionConsumerServiceLocation);

        // check if sign credentials are provided
        if (StringUtils.hasText(signingKey) && StringUtils.hasText(signingCertificate)) {
            //            // cleanup pem
            //            signingCertificate = cleanupPem("CERTIFICATE", signingCertificate);

            Saml2X509Credential signingCredentials = getCredentials(
                signingKey,
                signingCertificate,
                Saml2X509CredentialType.SIGNING,
                Saml2X509CredentialType.DECRYPTION
            );
            // add for signature
            builder.signingX509Credentials(c -> c.add(signingCredentials));

            // we use these also for decrypt
            builder.decryptionX509Credentials(c -> c.add(signingCredentials));
        }

        if (StringUtils.hasText(cryptKey) && StringUtils.hasText(cryptCertificate)) {
            // cleanup spaces, base64 encoding certs are expected
            //            cryptKey = cleanupPem("PRIVATE KEY", cryptKey);
            //            cryptCertificate = cleanupPem("CERTIFICATE", cryptCertificate);

            Saml2X509Credential cryptCredentials = getCredentials(
                cryptKey,
                cryptCertificate,
                Saml2X509CredentialType.ENCRYPTION,
                Saml2X509CredentialType.DECRYPTION
            );
            // add to decrypt credentials
            builder.decryptionX509Credentials(c -> c.add(cryptCredentials));
            // also use to encrypt messages
            builder.assertingPartyDetails(party -> party.encryptionX509Credentials(c -> c.add(cryptCredentials)));
        }

        return builder.build();
    }

    public String getMetadataUrl() {
        return "{baseUrl}/auth/" + getAuthority() + "/metadata/{registrationId}";
    }

    public String getAssertionConsumerUrl() {
        return "{baseUrl}/auth/" + getAuthority() + "/sso/{registrationId}";
    }

    public String getEntityId() {
        // let config override, this breaks some standards but saml...
        return configMap.getEntityId() != null ? configMap.getEntityId() : getMetadataUrl();
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

    public boolean requireEmailAddress() {
        return configMap.getRequireEmailAddress() != null ? configMap.getRequireEmailAddress().booleanValue() : false;
    }

    public String getSubAttributeName() {
        String subAttributeName = configMap.getSubAttributeName();
        return StringUtils.hasText(subAttributeName) ? subAttributeName : null;
    }

    //
    private Saml2X509Credential getVerificationCertificate(String certificate)
        throws CertificateException, IOException {
        return new Saml2X509Credential(CertificateParser.parseX509(certificate), Saml2X509CredentialType.VERIFICATION);
    }

    private Saml2X509Credential getCredentials(String key, String certificate, Saml2X509CredentialType... keyUse)
        throws IOException, CertificateException {
        //        PrivateKey pk = RsaKeyConverters.pkcs8().convert(new ByteArrayInputStream(key.getBytes()));
        PrivateKey pk = CertificateParser.parsePrivateWithUndefinedHeader(key);
        X509Certificate cert = CertificateParser.parseX509(certificate);
        return new Saml2X509Credential(pk, cert, keyUse);
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

    private Saml2MessageBinding getServiceBinding(String value) {
        Saml2MessageBinding ssoServiceBinding = Saml2MessageBinding.POST;
        if ("HTTP-Redirect".equals(value)) {
            ssoServiceBinding = Saml2MessageBinding.REDIRECT;
        }

        return ssoServiceBinding;
    }
}
