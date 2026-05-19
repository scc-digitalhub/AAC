package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.setup.AbstractSpidIdentityProviderTest;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class SpidIdentityProviderConfigurationTest extends AbstractSpidIdentityProviderTest {

    @Autowired
    private Environment env;

    @Autowired
    private BootstrapConfig config;

    @Autowired
    @Qualifier("spidProviderConfigRepository")
    private ProviderConfigRepository<SpidIdentityProviderConfig> spidProviderConfigRepository;

    /* =========================================================================
     * SPID Utility Components
     * ========================================================================= */
    protected MockMetadataIDP mockMetadataIDP = new MockMetadataIDP();

    @BeforeEach
    public void setupConfiguration() {
        initMockMvc();

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                initReamlByBoostrap(realm);

                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                assertThat(idps.size()).isEqualTo(2);

                ConfigurableIdentityProvider idp1 = idps.get(0);
                assertThat(idp1.getAuthority()).isNotNull();
                assertThat(idp1.getRealm()).isNotNull();
                assertThat(idp1.getConfiguration()).isNotNull();

                signingIdpAuthority = idp1.getAuthority();
                signingIdpProvider = idp1.getProvider();
                signingIdpSloUrl = BASE_URL + SLO_PATH + encodeRegistrationId(signingIdpProvider);

                SpidIdentityProviderConfigMap configmap = new SpidIdentityProviderConfigMap();
                configmap.setConfiguration(idp1.getConfiguration());

                assertThat(configmap.getSpidAttributes()).isNotNull();
                assertThat(configmap.getSigningCredentials()).isNotNull();
                assertThat(configmap.getSigningCredentials().get(1).getSigningKey()).isNotNull();
                assertThat(configmap.getSigningCredentials().get(1).getSigningCertificate()).isNotNull();
                assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isNotNull();

                signingSetSpidAttributes = configmap.getSpidAttributes();
                signingCredentials = configmap.getSigningCredentials();
                signingActiveSigningCredentialId = configmap.getActiveAuthRequestSigningCredentialId();
                signingIdpSigningKey = configmap.getSigningCredentials().get(1).getSigningKey();
                signingIdpSigningCertificate = configmap.getSigningCredentials().get(1).getSigningCertificate();
            }
        });
    }

    /**
     * Verifies that the base SPID configuration wrapper contains the expected
     * metadata URLs and cryptographic keys loaded from the environment.
     */
    @Test
    @DisplayName("Verifica configurazione base dell'Identity Provider SPID")
    public void testSpidConfigurationIsValid() throws Exception {
        // PROFILE-BASED ISOLATION: Thanks to the dedicated 'test-spid' Spring Profile and
        // the isolated H2 database, the repository returns a pristine configuration state
        // loaded directly from our test bootstrap. This ensures the test verifies the
        // setup without risking "dirtying" the global state of other test suites
        SpidIdentityProviderConfig spidIdentityProviderConfig = spidProviderConfigRepository.findByProviderId(signingIdpProvider);

        assertThat(spidIdentityProviderConfig.getProvider()).isEqualTo(signingIdpProvider);
        assertThat(spidIdentityProviderConfig.getStatusMap().getMetadataUrl()).isEqualTo(signingIdpMetadataUrl);
        assertThat(spidIdentityProviderConfig.getConfigMap().getSigningCredentials().get(1).getSigningKey()).isEqualTo(signingIdpSigningKey);
        assertThat(spidIdentityProviderConfig.getConfigMap().getSigningCredentials().get(1).getSigningCertificate()).isEqualTo(signingIdpSigningCertificate);
    }

    /**
     * Ensures that the Assertion Consumer Service (ACS) Location, where the IdP
     * will redirect the user after login, is correctly constructed.
     */
    @Test
    @DisplayName("Verifica URL dell'Assertion Consumer Service (ACS)")
    public void testAssertionConsumerServiceLocationIsValid() throws Exception {
        Set<RelyingPartyRegistration> rpRegistration = Collections.singleton(spidProviderConfigRepository.findByProviderId(signingIdpProvider).getRelyingPartyRegistration());
        assertThat(rpRegistration.size()).isEqualTo(1);

        for (RelyingPartyRegistration rpr : rpRegistration) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(rpr.getAssertionConsumerServiceLocation());
            assertThat(builder.buildAndExpand(Map.of("baseUrl", BASE_URL)).toUriString()).isEqualTo(signingIdpSsoUrl);
        }
    }

    /**
     * Validates the Spring Security RelyingPartyRegistration details, confirming
     * EntityID, registration ID, and the presence of dual X.509 signing credentials.
     */
    @Test
    @DisplayName("Verifica Relying Party Registration (Metadata SP)")
    public void testRelyingPartyRegistrationIsCorrect() throws Exception {
        RelyingPartyRegistration rpRegistration = spidProviderConfigRepository.findByProviderId(signingIdpProvider).getMetadataRelyingPartyRegistration();

        assertThat(rpRegistration).isNotNull();
        assertThat(rpRegistration.getEntityId()).isEqualTo(signingIdpEntityId);
        assertThat(rpRegistration.getRegistrationId()).isEqualTo(encodeRegistrationId(signingIdpProvider));
        assertThat(rpRegistration.getSigningX509Credentials().size()).isEqualTo(3);

        Saml2X509Credential credential = rpRegistration.getSigningX509Credentials().iterator().next();
        assertThat(credential).isNotNull();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(rpRegistration.getAssertionConsumerServiceLocation());
        assertThat(builder.buildAndExpand(Map.of("baseUrl", BASE_URL)).toUriString()).isEqualTo(signingIdpSsoUrl);
    }

    /**
     * Confirms that the target Asserting Party (IdP) Entity IDs are successfully
     * extracted and registered within the Relying Party configurations.
     */
    @Test
    @DisplayName("Verifica registrazione degli Identity Provider (IdP)")
    public void testIdentityProvidersAreRegistered() throws Exception {
        SpidIdentityProviderConfig spidIdentityProviderConfig = spidProviderConfigRepository.findByProviderId(signingIdpProvider);
        Set<RelyingPartyRegistration> relyingPartyRegistrations = Collections.singleton(spidIdentityProviderConfig.getRelyingPartyRegistration());

        assertThat(relyingPartyRegistrations.size()).isEqualTo(1);
        assertThat(relyingPartyRegistrations.stream().findFirst().get().getEntityId())
                .isEqualTo(signingIdpEntityId);
        assertThat(spidIdentityProviderConfig.getIdentityProviders())
                .extracting(SpidRegistration::getEntityId)
                .contains(mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_REDIRECT);
    }

    /**
     * Checks if the required SPID Attributes (e.g., name, familyName, fiscalNumber)
     * are correctly parsed from the configuration.
     */
    @Test
    @DisplayName("Verifica configurazione degli attributi SPID richiesti")
    public void testSpidAttributesAreConfigured() throws Exception {
        Set<SpidAttribute> attributes = spidProviderConfigRepository.findByProviderId(signingIdpProvider).getConfigMap().getSpidAttributes();
        assertThat(attributes.size()).isEqualTo(5);

        assertThat(attributes).isEqualTo(signingSetSpidAttributes);
    }

    /**
     * Validates that the underlying authentication authority type is explicitly set to 'spid'.
     */
    @Test
    @DisplayName("Verifica che l'Authority configurata sia 'spid'")
    public void testAuthorityIsSpid() throws Exception {
        String authority = spidProviderConfigRepository.findByProviderId(signingIdpProvider).getAuthority();
        assertThat(authority).isEqualTo(signingIdpAuthority);
        assertThat(authority).isEqualTo("spid");
    }

    /**
     * Verifies that the correct number of signing credentials (usually active + rollover)
     * are loaded in the configuration.
     */
    @Test
    @DisplayName("Verifica presenza delle credenziali di firma (X.509) - METADATA_EXPOSURE")
    public void testSigningCredentialsArePresent() throws Exception {
        Integer signingCredentialsListSize = spidProviderConfigRepository.findByProviderId(signingIdpProvider).getConfigMap().getSigningCredentials().size();
        assertThat(signingCredentialsListSize).isEqualTo(signingCredentials.size());
        assertThat(signingCredentials.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Errata Configurazione - Empty Credentials ")
    public void testEmptyCredentials() throws Exception {
        initConfigsInvalidServiceProvider();

        assertThrows(IllegalArgumentException.class, () -> {
            createInvalidSpidIdentityProvider(configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key missing in standalone credential")
    public void testKeyStandaloneCredential() throws Exception {
        initConfigsInvalidServiceProvider();
        configsInvalid.setSigningCertificate(signingIdpSigningCertificate);

        assertThrows(IllegalArgumentException.class, () -> {
            createInvalidSpidIdentityProvider(configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key missing in list credentials")
    public void testKeyListCredentials() throws Exception {
        initConfigsInvalidServiceProvider();

        signingListCredentialsInvalid.add(new SigningCredential(signingActiveSigningCredentialIdInvalid, null, signingIdpSigningCertificate));
        configsInvalid.setSigningCredentials(signingListCredentialsInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            createInvalidSpidIdentityProvider(configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Not Found Id Maching")
    public void testNotFoundIdMaching() throws Exception {
        initConfigsInvalidServiceProvider();

        signingListCredentialsInvalid.add(new SigningCredential(null, signingIdpSigningKey, signingIdpSigningCertificate));
        configsInvalid.setSigningCredentials(signingListCredentialsInvalid);
        configsInvalid.setActiveAuthRequestSigningCredentialId(signingActiveSigningCredentialIdInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            createInvalidSpidIdentityProvider(configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch in standalone credential")
    public void testKeyAndCertificateMismatchStandaloneCredential() throws Exception {
        initConfigsInvalidServiceProvider();

        configsInvalid.setSigningCertificate(signingIdpSigningCertificate);
        configsInvalid.setSigningKey(signingCredentials.get(0).getSigningKey());

        assertThrows(IllegalArgumentException.class, () -> {
            createInvalidSpidIdentityProvider(configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch in list credentials")
    public void testKeyAndCertificateMismatchListCredentials() throws Exception {
        initConfigsInvalidServiceProvider();

        signingListCredentialsInvalid.add(new SigningCredential(signingActiveSigningCredentialIdInvalid, signingCredentials.get(0).getSigningKey(), signingIdpSigningCertificate));
        configsInvalid.setSigningCredentials(signingListCredentialsInvalid);
        configsInvalid.setActiveAuthRequestSigningCredentialId(signingActiveSigningCredentialIdInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            createInvalidSpidIdentityProvider(configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch")
    public void testKeyAndCertificateMismatch() throws Exception {
        initConfigsInvalidServiceProvider();

        signingListCredentialsInvalid.add(new SigningCredential(signingActiveSigningCredentialIdInvalid, signingCredentials.get(0).getSigningKey(), signingIdpSigningCertificate));
        configsInvalid.setSigningCredentials(signingListCredentialsInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            createInvalidSpidIdentityProvider(configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Duplicate certificates")
    public void testDuplicateCertificates() throws Exception {
        initConfigsInvalidServiceProvider();

        signingListCredentialsInvalid.add(new SigningCredential(null, signingIdpSigningKey, signingIdpSigningCertificate));
        signingListCredentialsInvalid.add(new SigningCredential(null, signingCredentials.get(0).getSigningKey(), signingIdpSigningCertificate));
        configsInvalid.setSigningCredentials(signingListCredentialsInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            createInvalidSpidIdentityProvider(configsInvalid);
        });
    }
}
