package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;
import it.smartcommunitylab.aac.spid.model.SpidError;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockIdpSpid;
import it.smartcommunitylab.aac.spid.setupflow.SpidAgidAnomalyScenario;
import it.smartcommunitylab.aac.spid.utils.AgidUtils;
import it.smartcommunitylab.aac.spid.setupflow.SpidAgidErrorContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.MessageSource;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for SPID AgID anomaly scenarios and edge cases (Groups A, B, and C).
 * Verifies correct SAML response validation, specific error code propagation, and UI/backend error handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "test-spid"})
@EnableWireMock({
    // Setup two fixed-port WireMock servers, mapping them to their respective YAML configuration properties
    @ConfigureWireMock(port = 58838, name = "idp-server-redirect", property = "wiremock.idp.redirect.url"),
    @ConfigureWireMock(port = 58839, name = "idp-server-post", property = "wiremock.idp.post.url")
})
// Add @Transactional to clean up the DB automatically between @Test methods within this class
@Transactional
public class SpidAgidAnomalyTest extends BaseSpidTest {

    @Autowired
    private MessageSource messageSource;

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    protected WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    protected WireMockServer mockIdPServerPost;

    protected AgidUtils agidUtils = new AgidUtils();
    protected MockIdpSpid mockIdpSpid = new MockIdpSpid();
    protected IdentityProvider identityProvider = new IdentityProvider();

    @BeforeEach
    public void setupConfigurationAndMocks() {
        initMockMvc();
        mockIdpSpid.prepareMockMetadata(mockIdPServerRedirect, mockIdPServerPost);

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();

                // Any Identity Provider loaded from the bootstrap can be used here
                ConfigurableIdentityProvider idp = idps.get(1);

                identityProvider.initRealmByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);
                identityProvider.initRegistrationIdBinding(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    /* =========================================================================================
     * AGID SUCCESS SCENARIOS
     * ========================================================================================= */

    @Test
    @DisplayName("AgID CODE_01: Autenticazione corretta Binding (HTTP-Redirect)")
    public void testAgidCode01SuccessfulAuthenticationWithHttpRedirectBinding() throws Exception {
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        agidUtils.executeSuccessfulSamlFlow(context);
    }

    @Test
    @DisplayName("AgID CODE_26: Processo di erogazione identità pregressa a buon fine")
    public void testAgidCode26SuccessfulIdentityProvisioning() throws Exception {
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        agidUtils.executeSuccessfulSamlFlow(context);
    }

    @Test
    @Disabled("Riservata da specifiche AgID v1.4 (Nessuna implementazione richiesta)")
    @DisplayName("AgID CODE_24: Riservata")
    public void testAgidCode24IsReserved() {
    }

    /* =========================================================================================
     * ANOMALY SCENARIOS - TECHNICAL & IDP ERRORS
     * ========================================================================================= */

    @DisplayName("Test Anomalie SPID - GRUPPO B & C: Errori Tecnici e di Sistema (Backend + UI)")
    @ParameterizedTest(name = "AgID {0} - Propaga il codice anomalia esatto e lo renderizza sulla UI")
    @EnumSource(
        value = SpidAgidAnomalyScenario.class,
        names = {"CODE_08", "CODE_09", "CODE_11", "CODE_12", "CODE_13", "CODE_14", "CODE_15", "CODE_16", "CODE_17", "CODE_18"}
    )
    public void testAgidTechnicalAnomaliesAreHandledCorrectly(SpidAgidAnomalyScenario scenario) throws Exception {
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        SpidAuthenticationException spidEx = agidUtils.executeAnomalyScenarioAndGetException(scenario, context);

        agidUtils.validateSpidAnomalyTechnicalAndSystem(spidEx, mockMvc, LOGIN_DESTINATION_URL, messageSource);
        assertThat(spidEx.getError().getErrorCode()).isEqualTo(SpidError.SPID_FAILED_RESPONSE_VALIDATION.getErrorCode());
    }

    /* =========================================================================================
     * ANOMALY SCENARIOS - USER ERRORS
     * ========================================================================================= */

    @DisplayName("Test Anomalie SPID - GRUPPO A: Errori Utente (Backend + UI)")
    @ParameterizedTest(name = "AgID {0} - Mostra pagina di cortesia con messaggio specifico e codice")
    @EnumSource(
        value = SpidAgidAnomalyScenario.class,
        names = {"CODE_19", "CODE_20", "CODE_21", "CODE_22", "CODE_23", "CODE_25", "CODE_30"}
    )
    public void testAgidUserAnomaliesAreHandledCorrectly(SpidAgidAnomalyScenario scenario) throws Exception {
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        SpidAuthenticationException spidEx = agidUtils.executeAnomalyScenarioAndGetException(scenario, context);

        agidUtils.validateSpidAnomalyUser(scenario, spidEx, mockMvc, LOGIN_DESTINATION_URL, messageSource);

        // Map the scenario to the specific expected error
        SpidError expectedError = switch (scenario) {
            case CODE_19 -> SpidError.AUTH_FAILED_REQUEST_COUNT;
            case CODE_20 -> SpidError.AUTH_FAILED_INVALID_CREDENTIALS;
            case CODE_21 -> SpidError.AUTH_FAILED_TIMEOUT;
            case CODE_22 -> SpidError.AUTH_FAILED_NOT_APPROVED;
            case CODE_23 -> SpidError.AUTH_FAILED_USER_LOCKED;
            case CODE_25 -> SpidError.AUTH_FAILED_CANCELED;
            case CODE_30 -> SpidError.AUTH_FAILED_WRONG_IDENTITY_TYPE;
            default -> throw new IllegalStateException("Unexpected user scenario: " + scenario);
        };

        // Verify that the exception contains the exact error code (e.g., "19", "20", etc.)
        assertThat(spidEx.getError().getErrorCode()).isEqualTo(expectedError.getErrorCode());
    }

    /* =========================================================================================
     * ANOMALY SCENARIOS - IDENTITY PROVISIONING
     * ========================================================================================= */

    @DisplayName("Test Anomalie SPID - GRUPPO C: Riuso Identità (Solo gestione Backend)")
    @ParameterizedTest(name = "AgID {0} - Gestisce il ritorno dall'IdP senza obbligo di pagina di cortesia specifica")
    @EnumSource(
        value = SpidAgidAnomalyScenario.class,
        names = {"CODE_27", "CODE_28", "CODE_29"}
    )
    public void testAgidIdentityProvisioningAnomaliesAreHandledCorrectly(SpidAgidAnomalyScenario scenario) throws Exception {
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        SpidAuthenticationException spidEx = agidUtils.executeAnomalyScenarioAndGetException(scenario, context);

        assertThat(spidEx.getError().getErrorCode()).isEqualTo(SpidError.SPID_FAILED_RESPONSE_VALIDATION.getErrorCode());
    }

    // ==========================================================
    // TEST WITH HTTP-POST BINDING
    // ==========================================================

    @Test
    @DisplayName("AgID CODE_01: Autenticazione corretta Binding (HTTP-POST)")
    public void testAgidCode01SuccessfulAuthenticationWithHttpPostBinding() throws Exception {
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(true)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdPost)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST)
            .build();

        agidUtils.executeSuccessfulSamlFlow(context);
    }

    /**
     * Helper method to initialize a base context builder with common parameters
     * shared across most tests.
     */
    private SpidAgidErrorContextBuilder.Builder getBaseContextBuilder() {
        return SpidAgidErrorContextBuilder.builder()
            .mockMvc(mockMvc)
            .xmlTemplate(mockIdpSpid.XML_RESPONSE_TEMPLATE)
            .xmlAgidErrorTemplate(mockIdpSpid.XML_RESPONSE_AGID_ERROR_TEMPLATE)
            .baseUrlAac(BASE_URL)
            .userDestinationUrl(USER_DESTINATION_URL)
            .authenticatePath(AUTHENTICATE_PATH)
            .loginDestinationUrl(LOGIN_DESTINATION_URL)
            .entityIdAac(identityProvider.signingIdpEntityId)
            .idpPrivateKey(mockIdpSpid.IDP_MOCK_PRIVATE_KEY)
            .idpCertificate(mockIdpSpid.IDP_MOCK_CERTIFICATE);
    }
}
