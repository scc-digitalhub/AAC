package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.provider.AbstractIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.FirstIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.InvalidIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderStatusMap;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockMetadataIDP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test suite for validating the SPID configuration loading and setup.
 * It ensures that Relying Party Registrations, Identity Providers, X.509 Certificates,
 * and SPID attributes are correctly initialized in the Spring context before
 * actual SAML authentications are performed.
 */
@SpringBootTest
@AutoConfigureMockMvc
// Loads the base profile ("test") and then applies SPID overrides ("test-spid")
@ActiveProfiles({"test", "test-spid"})
// Add @Transactional to clean up the DB automatically between @Test methods within this class
@Transactional
public class SpidIdentityProviderConfigurationTest extends BaseSpidTest {

    @Autowired
    private Environment env;

    @Autowired
    private BootstrapConfig config;

    @Autowired
    @Qualifier("spidProviderConfigRepository")
    private ProviderConfigRepository<SpidIdentityProviderConfig> spidProviderConfigRepository;

    protected MockMetadataIDP mockMetadataIDP = new MockMetadataIDP();
    protected FirstIdentityProvider firstIdentityProvider = new FirstIdentityProvider();
    protected InvalidIdentityProvider invalidIdentityProvider = new InvalidIdentityProvider();

    @BeforeEach
    public void setupConfiguration() {
        initMockMvc();

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                ConfigurableIdentityProvider idp = idps.get(0);
                firstIdentityProvider.initReamlByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);

                firstIdentityProvider.signingIdpAuthority = idp.getAuthority();
                firstIdentityProvider.signingIdpProvider = idp.getProvider();
                firstIdentityProvider.signingIdpSloUrl = BASE_URL + SLO_PATH + AbstractIdentityProvider.encodeRegistrationId(firstIdentityProvider.signingIdpProvider);

                SpidIdentityProviderConfigMap configmap = new SpidIdentityProviderConfigMap();
                configmap.setConfiguration(idp.getConfiguration());

                firstIdentityProvider.signingSetSpidAttributes = configmap.getSpidAttributes();
                firstIdentityProvider.signingCredentials = configmap.getSigningCredentials();
                firstIdentityProvider.signingActiveSigningCredentialId = configmap.getActiveAuthRequestSigningCredentialId();
                firstIdentityProvider.signingIdpSigningKey = configmap.getSigningCredentials().get(1).getSigningKey();
                firstIdentityProvider.signingIdpSigningCertificate = configmap.getSigningCredentials().get(1).getSigningCertificate();
            }
        });
    }

    /* =========================================================================
     * INTEGRATION TESTS FOR URLS AND MAPS
     * ========================================================================= */

    @Test
    @DisplayName("Verifica generazione degli URL SPID (Metadata, SSO, SLO)")
    public void testUrlGeneration() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider);
        assertThat(config).isNotNull();

        config.setBaseUrl(BASE_URL);
        String expectedEncodedId = SpidIdentityProviderConfig.encodeRegistrationId(firstIdentityProvider.signingIdpProvider);

        assertThat(config.getMetadataUrl()).isEqualTo(BASE_URL + "/auth/spid/metadata/" + expectedEncodedId);
        assertThat(config.getAssertionConsumerUrl()).isEqualTo(BASE_URL + "/auth/spid/sso/" + expectedEncodedId);
        assertThat(config.getConsumerUrl()).isEqualTo("{baseUrl}/auth/spid/sso/" + expectedEncodedId);
        assertThat(config.getLogoutUrl()).isEqualTo("{baseUrl}/auth/spid/slo/" + expectedEncodedId);
    }

    @Test
    @DisplayName("Verifica Status Map e mappa della Metadata Configuration")
    public void testStatusMapAndMetadataConfiguration() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider);
        config.setBaseUrl(BASE_URL);

        SpidIdentityProviderStatusMap statusMap = config.getStatusMap();
        assertThat(statusMap).isNotNull();
        assertThat(statusMap.getMetadataUrl()).isEqualTo(config.getMetadataUrl());
        assertThat(statusMap.getAssertionConsumerUrl()).isEqualTo(config.getAssertionConsumerUrl());

        Map<String, String> metaConfigMap = config.getMetadataConfiguration();
        assertThat(metaConfigMap).isNotNull();
        assertThat(metaConfigMap.get("entityId")).isEqualTo(config.getEntityId());
        assertThat(metaConfigMap.get("organizationName")).isEqualTo("TN Provincia Test");
        assertThat(metaConfigMap.get("organizationDisplayName")).isEqualTo("Provincia Test");
        assertThat(metaConfigMap.get("contactPersonEmailAddress")).isEqualTo("tecnico@provincia-test.it");
        assertThat(metaConfigMap.get("contactPersonIpaCode")).isEqualTo("codice_ipa_test");

        assertThat(metaConfigMap).containsKey("attributeConsumingServiceIndex 0");
    }

    @Test
    @DisplayName("Verifica Getters diretti per Organizzazione e Contatti")
    public void testOrganizationAndContactGetters() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider);

        assertThat(config.getOrganizationName()).isEqualTo("TN Provincia Test");
        assertThat(config.getOrganizationDisplayName()).isEqualTo("Provincia Test");
        assertThat(config.getOrganizationUrl()).isEqualTo("https://www.tn-provincia-test.it");
        assertThat(config.getContactPersonEmailAddress()).isEqualTo("tecnico@provincia-test.it");
        assertThat(config.getContactPersonIPACode()).isEqualTo("codice_ipa_test");

        assertThat(config.getRelyingPartyRegistrationIsForceAuthn()).isTrue();
    }

    @Test
    @DisplayName("Verifica Attributi di Mapping Utente (Username e Sub)")
    public void testUserMappingAttributes() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider);

        assertThat(config.getUsernameAttributeName().name()).isEqualTo("FISCAL_NUMBER");
        assertThat(config.getSubAttributeName().name()).isEqualTo("FISCAL_NUMBER");
    }

    /* =========================================================================
     * INTEGRATION TESTS FOR SPRING SECURITY / SAML
     * ========================================================================= */

    @Test
    @DisplayName("Verifica generazione Credenziali OpenSAML per firma Metadata")
    public void testMetadataSigningCredentials() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider);

        List<org.opensaml.security.credential.Credential> credentials = config.getMetadataRelyingPartySigningCredentials();

        assertThat(credentials).isNotNull();
        assertThat(credentials).hasSize(1);

        org.opensaml.security.credential.Credential cred = credentials.get(0);
        assertThat(cred.getEntityId()).isEqualTo(config.getEntityId());
        assertThat(cred.getUsageType()).isEqualTo(org.opensaml.security.credential.UsageType.SIGNING);
        assertThat(cred.getPublicKey()).isNotNull();
        assertThat(cred.getPrivateKey()).isNotNull();
    }

    @Test
    @DisplayName("Verifica collezione dei Relying Party Registration IDs")
    public void testRelyingPartyRegistrationIds() {
        SpidIdentityProviderConfig config = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider);
        Set<String> ids = config.getRelyingPartyRegistrationIds();

        assertThat(ids).isNotNull();
        assertThat(ids).isNotEmpty();

        String expectedMetadataRegId = SpidIdentityProviderConfig.encodeRegistrationId(config.getProvider());
        assertThat(ids).contains(expectedMetadataRegId);
    }

    @Test
    @DisplayName("Verifica Relying Party Registration (Metadata SP)")
    public void testRelyingPartyRegistrationIsCorrect() throws Exception {
        RelyingPartyRegistration rpRegistration = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider).getMetadataRelyingPartyRegistration();

        assertThat(rpRegistration).isNotNull();
        assertThat(rpRegistration.getEntityId()).isEqualTo(firstIdentityProvider.signingIdpEntityId);
        assertThat(rpRegistration.getRegistrationId()).isEqualTo(AbstractIdentityProvider.encodeRegistrationId(firstIdentityProvider.signingIdpProvider));
        assertThat(rpRegistration.getSigningX509Credentials().size()).isEqualTo(3);

        Saml2X509Credential credential = rpRegistration.getSigningX509Credentials().iterator().next();
        assertThat(credential).isNotNull();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(rpRegistration.getAssertionConsumerServiceLocation());
        assertThat(builder.buildAndExpand(Map.of("baseUrl", BASE_URL)).toUriString()).isEqualTo(firstIdentityProvider.signingIdpSsoUrl);
    }

    @Test
    @DisplayName("Verifica registrazione degli Identity Provider (IdP)")
    public void testIdentityProvidersAreRegistered() throws Exception {
        SpidIdentityProviderConfig spidIdentityProviderConfig = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider);
        Set<RelyingPartyRegistration> relyingPartyRegistrations = Collections.singleton(spidIdentityProviderConfig.getRelyingPartyRegistration());

        assertThat(relyingPartyRegistrations.size()).isEqualTo(1);
        assertThat(relyingPartyRegistrations.stream().findFirst().get().getEntityId())
            .isEqualTo(firstIdentityProvider.signingIdpEntityId);
        assertThat(spidIdentityProviderConfig.getIdentityProviders())
            .extracting(SpidRegistration::getEntityId)
            .contains(mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_REDIRECT);
    }

    /* =========================================================================
     * CRYPTOGRAPHIC MISMATCH TESTS (SPRING SECURITY VALIDATION)
     * ========================================================================= */

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch in standalone credential")
    public void testKeyAndCertificateMismatchStandaloneCredential() throws Exception {
        invalidIdentityProvider.reset();

        invalidIdentityProvider.configsInvalid.setSigningCertificate(firstIdentityProvider.signingIdpSigningCertificate);
        invalidIdentityProvider.configsInvalid.setSigningKey(firstIdentityProvider.signingCredentials.get(0).getSigningKey());

        assertThrows(IllegalArgumentException.class, () -> {
            invalidIdentityProvider.createInvalidSpidIdentityProvider(firstIdentityProvider.signingIdpAuthority, invalidIdentityProvider.configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch in list credentials")
    public void testKeyAndCertificateMismatchListCredentials() throws Exception {
        invalidIdentityProvider.reset();

        invalidIdentityProvider.signingListCredentialsInvalid.add(new SigningCredential(invalidIdentityProvider.signingActiveSigningCredentialIdInvalid, firstIdentityProvider.signingCredentials.get(0).getSigningKey(), firstIdentityProvider.signingIdpSigningCertificate));
        invalidIdentityProvider.configsInvalid.setSigningCredentials(invalidIdentityProvider.signingListCredentialsInvalid);
        invalidIdentityProvider.configsInvalid.setActiveAuthRequestSigningCredentialId(invalidIdentityProvider.signingActiveSigningCredentialIdInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            invalidIdentityProvider.createInvalidSpidIdentityProvider(firstIdentityProvider.signingIdpAuthority, invalidIdentityProvider.configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch")
    public void testKeyAndCertificateMismatch() throws Exception {
        invalidIdentityProvider.reset();

        invalidIdentityProvider.signingListCredentialsInvalid.add(new SigningCredential(invalidIdentityProvider.signingActiveSigningCredentialIdInvalid, firstIdentityProvider.signingCredentials.get(0).getSigningKey(), firstIdentityProvider.signingIdpSigningCertificate));
        invalidIdentityProvider.configsInvalid.setSigningCredentials(invalidIdentityProvider.signingListCredentialsInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            invalidIdentityProvider.createInvalidSpidIdentityProvider(firstIdentityProvider.signingIdpAuthority, invalidIdentityProvider.configsInvalid);
        });
    }
}
