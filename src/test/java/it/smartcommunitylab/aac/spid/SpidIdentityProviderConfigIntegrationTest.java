package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SigningCredentialHelper;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockIdpSpid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for verifying the internal configuration and setup of the SPID Identity Provider.
 * Validates accurate URL generation (SSO, SLO, Metadata), OpenSAML credential mapping, certificate extraction,
 * and the correct initialization of Spring Security's Relying on Party and Identity Provider registrations.
 */
@SpringBootTest
@ActiveProfiles({"test", "test-spid"})
public class SpidIdentityProviderConfigIntegrationTest extends BaseSpidTest {

    protected MockIdpSpid mockIdpSpid = new MockIdpSpid();
    protected IdentityProvider identityProvider = new IdentityProvider();

    @BeforeEach
    public void setupConfiguration() {
        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();

                // Every identity provider is supported
                ConfigurableIdentityProvider idp = idps.get(1);

                identityProvider.initRealmByBootstrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);
                identityProvider.initRegistrationIdBinding(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    /* =========================================================================
     * INTEGRATION TESTS FOR URLS AND MAPS
     * ========================================================================= */

    @Test
    @DisplayName("Verifica generazione degli URL SPID (Metadata, SSO, SLO)")
    public void testUrlGeneration() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider);
        assertThat(config).isNotNull();

        config.setBaseUrl(BASE_URL);
        String expectedEncodedId = SpidIdentityProviderConfig.encodeRegistrationId(identityProvider.signingIdpProvider);

        assertThat(config.getMetadataUrl()).isEqualTo(BASE_URL + "/auth/spid/metadata/" + expectedEncodedId);
        assertThat(config.getAssertionConsumerUrl()).isEqualTo(BASE_URL + "/auth/spid/sso/" + expectedEncodedId);
        assertThat(config.getConsumerUrl()).isEqualTo("{baseUrl}/auth/spid/sso/" + expectedEncodedId);
        assertThat(config.getLogoutUrl()).isEqualTo("{baseUrl}/auth/spid/slo/" + expectedEncodedId);
    }

    /* =========================================================================
     * INTEGRATION TESTS FOR SPRING SECURITY / SAML
     * ========================================================================= */

    @Test
    @DisplayName("Verifica collezione dei Relying Party Registration IDs")
    public void testRelyingPartyRegistrationIds() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider);
        Set<String> relyingPartyRegistrationIds = config.getRelyingPartyRegistrationIds();

        assertThat(relyingPartyRegistrationIds).isNotEmpty();

        Set<String> expectedRelyingPartyRegistrationIds = new HashSet<>();
        expectedRelyingPartyRegistrationIds.add(SpidIdentityProviderConfig.encodeRegistrationId(config.getProvider()));
        expectedRelyingPartyRegistrationIds.add(identityProvider.registrationIdRedirect);
        expectedRelyingPartyRegistrationIds.add(identityProvider.registrationIdPost);

        assertThat(relyingPartyRegistrationIds).containsExactlyInAnyOrderElementsOf(expectedRelyingPartyRegistrationIds);
    }

    @Test
    @DisplayName("Verifica collezione dei Identity Providers")
    public void testIdentityProviders() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider);
        Set<SpidRegistration> spidRegistrationSet = config.getIdentityProviders();

        assertThat(spidRegistrationSet).isNotEmpty();
        assertThat(spidRegistrationSet)
            .extracting(SpidRegistration::getEntityId)
            .containsExactlyInAnyOrder(
                    "https://idp.identityserver.redirect",
                    "https://idp.identityserver.post"
            );

        Set<String> expectedIdentityProviders = new HashSet<>();
        expectedIdentityProviders.add(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT);
        expectedIdentityProviders.add(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST);

        assertThat(spidRegistrationSet)
            .extracting(SpidRegistration::getEntityId)
            .containsExactlyInAnyOrderElementsOf(expectedIdentityProviders);
    }

    @Test
    @DisplayName("Verifica Relying Party Registration (Metadata SP) - METADATA_EXPOSURE")
    public void testMetadataRelyingPartyRegistration() throws Exception {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider);
        RelyingPartyRegistration rpRegistration = config.getMetadataRelyingPartyRegistration();

        assertThat(rpRegistration).isNotNull();
        assertThat(rpRegistration.getEntityId()).isEqualTo(identityProvider.signingIdpEntityId);
        assertThat(rpRegistration.getRegistrationId()).isEqualTo(IdentityProvider.encodeBase64(identityProvider.signingIdpProvider));

        // While not strictly required by AGID guidelines, we enforce a minimum of 2 signing
        // credentials by design. This internal choice ensures higher consistency and supports
        // smooth certificate rotation (rollover) for exposed metadata.
        assertThat(config.getConfigMap().getSigningCredentials()).hasSizeGreaterThanOrEqualTo(2);

        List<SigningCredential> listSigningCredentials = SigningCredentialHelper.signingCredentialList(
                spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
                SigningCredentialHelper.CredentialPurpose.METADATA_EXPOSURE);

        List<String> expectedCertificates = new ArrayList<>();
        for (SigningCredential signingCredential : listSigningCredentials) {
            if (signingCredential.getSigningCertificate() != null) {
                expectedCertificates.add(signingCredential.getSigningCertificate());
            }
        }
        assertThat(rpRegistration.getSigningX509Credentials().size()).isEqualTo(expectedCertificates.size());

        Saml2X509Credential credential = rpRegistration.getSigningX509Credentials().iterator().next();
        assertThat(credential).isNotNull();

        String rawLocation = rpRegistration.getAssertionConsumerServiceLocation();

        // FIX: Manually expand the template and use java.net.URI to avoid fromUriString() SAST alerts.
        String expandedLocation = rawLocation.replace("{baseUrl}", BASE_URL);
        URI safeUri = URI.create(expandedLocation);

        assertThat(safeUri.toString()).isEqualTo(identityProvider.signingIdpSsoUrl);
    }

    @Test
    @DisplayName("Verifica generazione Credenziali OpenSAML per firma Metadata - METADATA_SIGNATURE")
    public void testMetadataRelyingPartySigningCredentials() throws Exception {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider);
        List<Credential> credentials = config.getMetadataRelyingPartySigningCredentials();

        assertThat(credentials).hasSize(1);

        Credential cred = credentials.get(0);
        assertThat(cred.getEntityId()).isEqualTo(config.getEntityId());
        assertThat(cred.getUsageType()).isEqualTo(UsageType.SIGNING);

        SigningCredential signingCredential = SigningCredentialHelper.signingCredentialList(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
            SigningCredentialHelper.CredentialPurpose.METADATA_SIGNATURE).get(0);

        assertThat(cred.getPublicKey()).isNotNull();
        assertThat(cred.getPrivateKey()).isNotNull();

        String cleanCertBase64 = signingCredential.getSigningCertificate()
                .replaceAll("-----[A-Z ]+-----", "")
                .replaceAll("\\s+", "");

        byte[] certDer = Base64.getDecoder().decode(cleanCertBase64);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate expectedCert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certDer));

        String actualPublicKeyBase64 = Base64.getEncoder().encodeToString(cred.getPublicKey().getEncoded());
        String expectedPublicKeyBase64 = Base64.getEncoder().encodeToString(expectedCert.getPublicKey().getEncoded());

        // CERTIFICATE VERIFICATION
        assertThat(actualPublicKeyBase64).isEqualTo(expectedPublicKeyBase64);

        String actualPrivateKeyBase64 = Base64.getEncoder().encodeToString(cred.getPrivateKey().getEncoded());
        String expectedPrivateKeyBase64 = signingCredential.getSigningKey()
                .replaceAll("-----[A-Z ]+-----", "")
                .replaceAll("\\s+", "");

        // PRIVATE KEY VERIFICATION
        assertThat(actualPrivateKeyBase64).isEqualTo(expectedPrivateKeyBase64);
    }

    @Test
    @DisplayName("Verifica registrazione Identity Provider - AUTH_REQUEST")
    public void testRelyingPartyRegistrationForAuthRequest() throws Exception {
        SpidIdentityProviderConfig spidIdentityProviderConfig = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider);
        RelyingPartyRegistration relyingPartyRegistration = spidIdentityProviderConfig.getRelyingPartyRegistration();

        assertThat(relyingPartyRegistration).isNotNull();
        assertThat(relyingPartyRegistration.getEntityId()).isEqualTo(identityProvider.signingIdpEntityId);

        SigningCredential signingCredential = SigningCredentialHelper.signingCredentialList(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
            SigningCredentialHelper.CredentialPurpose.AUTH_REQUEST).get(0);

        Saml2X509Credential saml2X509Credential = relyingPartyRegistration.getSigningX509Credentials().iterator().next();

        // CERTIFICATE
        String actualCertBase64 = Base64.getEncoder().encodeToString(saml2X509Credential.getCertificate().getEncoded());
        String expectedCertBase64 = signingCredential.getSigningCertificate()
            .replaceAll("-----[A-Z ]+-----", "")
            .replaceAll("\\s+", "");

        // CERTIFICATE VERIFICATION
        assertThat(actualCertBase64).isEqualTo(expectedCertBase64);

        // PRIVATE KEY
        String actualPrivateKeyBase64 = Base64.getEncoder().encodeToString(saml2X509Credential.getPrivateKey().getEncoded());
        String expectedPrivateKeyBase64 = signingCredential.getSigningKey()
            .replaceAll("-----[A-Z ]+-----", "")
            .replaceAll("\\s+", "");

        // PRIVATE KEY VERIFICATION
        assertThat(actualPrivateKeyBase64).isEqualTo(expectedPrivateKeyBase64);
    }
}
