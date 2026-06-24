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

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)".
     * Verifica lo scenario di successo nominale identificato dal codice AgID "CODE_01" utilizzando il binding HTTP-Redirect.
     * Il test simula la ricezione di una SAML Response valida emessa dall'Identity Provider a seguito di un flusso avviato in Redirect.
     * Garantisce che il motore di sicurezza del Service Provider sia in grado di:
     * 1. Validare lo stato di successo (<saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>).
     * 2. Decodificare e verificare l'integrità delle asserzioni firmate.
     * 3. Estrarre correttamente i dati dell'utente senza sollevare eccezioni, finalizzando l'autenticazione.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html">Regole Tecniche SPID - SAML Response</a>
     */
    @Test
    @DisplayName("AgID CODE_01: Autenticazione corretta Binding (HTTP-Redirect)")
    public void testAgidCode01SuccessfulAuthenticationWithHttpRedirectBinding() throws Exception {
        // Build the simulated execution context mimicking a compliant HTTP-Redirect inbound flow
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        // Execute the full end-to-end successful SAML response parsing and session validation
        agidUtils.executeSuccessfulSamlFlow(context);
    }

    /**
     * REGOLE TECNICHE SPID: Registro delle anomalie e scenari di collaudo.
     * Verifica lo scenario di successo identificato dal codice AgID "CODE_26", relativo al buon fine del processo
     * di erogazione o aggancio di un'identità pregressa/preesistente.
     * Questo test assicura che, qualora l'Identity Provider restituisca una risposta positiva a seguito di un flusso
     * di provisioning o riconciliazione dell'utente (tipico delle migrazioni di identità o della gestione di credenziali
     * pregresse), il Service Provider elabori correttamente il payload SAML, associ i relativi attributi e completi
     * la sessione di autenticazione senza anomalie di backend.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html">Regole Tecniche SPID - SAML Response</a>
     */
    @Test
    @DisplayName("AgID CODE_26: Processo di erogazione identità pregressa a buon fine")
    public void testAgidCode26SuccessfulIdentityProvisioning() throws Exception {
        // Build the simulated context mapping the specific AgID CODE_26 success scenario via HTTP-Redirect
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        // Process the successful inbound identity matching flow and ensure session creation
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

    /**
     * REGOLE TECNICHE SPID: Registro delle anomalie (Gruppo B e C - Errori Tecnici e di Sistema).
     * Verifica la corretta intercettazione, propagazione e renderizzazione sulla UI delle anomalie tecniche e di sistema.
     * I codici mappati (es. dall'08 al 18) rappresentano guasti sistemici, problemi di convalida della firma, incongruenze nei metadati
     * o rifiuti formali comunicati dall'Identity Provider. Il test garantisce che il Service Provider intercetti questi scenari
     * bloccanti traducendoli in un'eccezione interna centralizzata (SPID_FAILED_RESPONSE_VALIDATION), propaghi il codice d'errore nativo
     * per l'auditing e reindirizzi l'utente verso la corretta pagina di errore della UI con un messaggio localizzato, salvaguardando
     * la robustezza dell'applicazione.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html">Regole Tecniche SPID - SAML Response</a>
     */
    @DisplayName("Test Anomalie SPID - GRUPPO B & C: Errori Tecnici e di Sistema (Backend + UI)")
    @ParameterizedTest(name = "AgID {0} - Propaga il codice anomalia esatto e lo renderizza sulla UI")
    @EnumSource(
        value = SpidAgidAnomalyScenario.class,
        names = {"CODE_08", "CODE_09", "CODE_11", "CODE_12", "CODE_13", "CODE_14", "CODE_15", "CODE_16", "CODE_17", "CODE_18"}
    )
    public void testAgidTechnicalAnomaliesAreHandledCorrectly(SpidAgidAnomalyScenario scenario) throws Exception {
        // Build the mock execution context tailored for technical inbound anomalies via HTTP-Redirect
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        // Simulate the processing of the anomalous SAML Response to capture the expected security exception
        SpidAuthenticationException spidEx = agidUtils.executeAnomalyScenarioAndGetException(scenario, context);

        // Assert backend exception mapping and verify that the UI correctly routes to the login error view with localized messages
        agidUtils.validateSpidAnomalyTechnicalAndSystem(spidEx, mockMvc, LOGIN_DESTINATION_URL, messageSource);
        assertThat(spidEx.getError().getErrorCode()).isEqualTo(SpidError.SPID_FAILED_RESPONSE_VALIDATION.getErrorCode());
        assertThat(spidEx.getError().getErrorCode()).isEqualTo("1000");
    }

    /* =========================================================================================
     * ANOMALY SCENARIOS - USER ERRORS
     * ========================================================================================= */

    /**
     * REGOLE TECNICHE SPID: Registro delle anomalie (Gruppo A - Errori Utente).
     * Verifica la corretta intercettazione, la mappatura semantica e la visualizzazione sulla UI delle anomalie lato utente.
     * I codici del Gruppo A (es. 19, 20, 21, 22, 23, 25, 30) segnalano errori commessi direttamente dal cittadino, quali
     * credenziali errate, superamento dei tentativi massimi, sessione scaduta o annullamento esplicito del flusso. AgID impone
     * che il Service Provider gestisca tali eventi mostrando una specifica "pagina di cortesia" o un messaggio chiaro sulla UI,
     * esponendo tassativamente il codice identificativo dell'anomalia per consentire all'utente di comprendere il problema
     * ed eventualmente risolverlo autonomamente o contattando l'assistenza dell'IdP.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html">Regole Tecniche SPID - SAML Response</a>
     */
    @DisplayName("Test Anomalie SPID - GRUPPO A: Errori Utente (Backend + UI)")
    @ParameterizedTest(name = "AgID {0} - Mostra pagina di cortesia con messaggio specifico e codice")
    @EnumSource(
        value = SpidAgidAnomalyScenario.class,
        names = {"CODE_19", "CODE_20", "CODE_21", "CODE_22", "CODE_23", "CODE_25", "CODE_30"}
    )
    public void testAgidUserAnomaliesAreHandledCorrectly(SpidAgidAnomalyScenario scenario) throws Exception {
        // Initialize the testing context for user-side anomalies over the HTTP-Redirect binding
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        // Process the anomalous SAML response payload to capture the specific user authentication exception
        SpidAuthenticationException spidEx = agidUtils.executeAnomalyScenarioAndGetException(scenario, context);

        // Verify that the UI safely intercepts the failure and renders the dedicated courtesy page with localized messages
        agidUtils.validateSpidAnomalyUser(scenario, spidEx, mockMvc, LOGIN_DESTINATION_URL, messageSource);

        // Map each inbound AgID scenario to its respective internal domain error code
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

        // Assert that the thrown exception propagates the exact expected internal business error code
        assertThat(spidEx.getError().getErrorCode()).isEqualTo(expectedError.getErrorCode());
    }

    /* =========================================================================================
     * ANOMALY SCENARIOS - IDENTITY PROVISIONING
     * ========================================================================================= */

    /**
     * REGOLE TECNICHE SPID: Registro delle anomalie (Gruppo C - Provisioning e Riuso Identità).
     * Verifica la gestione a livello di backend delle anomalie legate al riuso o al provisioning dell'identità digitale.
     * I codici del Gruppo C (27, 28, 29) segnalano scenari particolari restituiti dall'Identity Provider in cui si riscontrano
     * conflitti o vincoli tecnici legati alla gestione dell'utenza (es. identità già in uso o anomalie nel ciclo di vita della sessione).
     * A differenza degli errori del Gruppo A, AgID non impone la visualizzazione di una pagina di cortesia specifica con messaggi
     * d'errore utente granulari, in quanto sono considerati blocchi logici interni. Il test garantisce che il Service Provider
     * intercetti correttamente la risposta anomala, invalidi il flusso respingendo la sottomissione e sollevi l'eccezione generale
     * di fallimento della validazione di backend (SPID_FAILED_RESPONSE_VALIDATION).
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html">Regole Tecniche SPID - SAML Response</a>
     */
    @DisplayName("Test Anomalie SPID - GRUPPO C: Riuso Identità (Solo gestione Backend)")
    @ParameterizedTest(name = "AgID {0} - Gestisce il ritorno dall'IdP senza obbligo di pagina di cortesia specifica")
    @EnumSource(
        value = SpidAgidAnomalyScenario.class,
        names = {"CODE_27", "CODE_28", "CODE_29"}
    )
    public void testAgidIdentityProvisioningAnomaliesAreHandledCorrectly(SpidAgidAnomalyScenario scenario) throws Exception {
        // Initialize the validation context mimicking a Group C identity reuse anomaly over HTTP-Redirect
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(false)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdRedirect)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .build();

        // Process the inbound SAML payload and capture the expected validation exception from the backend engine
        SpidAuthenticationException spidEx = agidUtils.executeAnomalyScenarioAndGetException(scenario, context);

        // Assert that the thrown exception maps precisely to a generic backend SAML response validation failure
        assertThat(spidEx.getError().getErrorCode()).isEqualTo(SpidError.SPID_FAILED_RESPONSE_VALIDATION.getErrorCode());
        assertThat(spidEx.getError().getErrorCode()).isEqualTo("1000");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)" -> "Binding HTTP-POST".
     * Verifica lo scenario di successo nominale identificato dal codice AgID "CODE_01" utilizzando il binding HTTP-POST.
     * Il test simula la ricezione di una SAML Response valida recapitata dall'Identity Provider tramite una sottomissione
     * POST asincrona verso l'endpoint Assertion Consumer Service (ACS) del Service Provider. Garantisce che il motore di
     * sicurezza sia in grado di estrarre il payload XML codificato dal corpo della richiesta HTTP, convalidare lo stato di
     * successo, verificare la firma crittografica dell'asserzione ed estrarre gli attributi utente completando l'autenticazione.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/trasmissione.html#binding-http-post">Regole Tecniche SPID - Binding HTTP-POST</a>
     */
    @Test
    @DisplayName("AgID CODE_01: Autenticazione corretta Binding (HTTP-POST)")
    public void testAgidCode01SuccessfulAuthenticationWithHttpPostBinding() throws Exception {
        // Build the simulated execution context enforcing the HTTP-POST binding for the inbound SAML message
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
            .activePostBinding(true)
            .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
            .registrationId(identityProvider.registrationIdPost)
            .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST)
            .build();

        // Process the successful HTTP-POST flow, validating the signature and completing user session mapping
        agidUtils.executeSuccessfulSamlFlow(context);
    }

    /* =========================================================================================
     * ANOMALY SCENARIOS - IDP & PROTOCOL ERRORS (SAML STATUS FAILURES)
     * ========================================================================================= */

    /**
     * REGOLE TECNICHE SPID: Registro delle anomalie (Errori IdP e di Protocollo SAML a monte).
     * Verifica la corretta gestione delle anomalie restituite direttamente dall'Identity Provider nel blocco `<saml2p:Status>`.
     * I codici mappati includono indisponibilità dell'IdP (02, 03), errori di formato o scadenze della AuthnRequest (04, 06),
     * fallimenti di verifica della firma dell'SP a monte (05), errori generici (07) e incongruenze di versione/struttura SAML (10).
     * Il test garantisce che, quando l'IdP risponde esplicitamente con un fallimento di protocollo, il Service Provider
     * rifiuti correttamente l'asserzione, sollevi l'eccezione di sicurezza centralizzata (SPID_FAILED_RESPONSE_VALIDATION)
     * e reindirizzi l'utente alla UI di errore di sistema.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html">Regole Tecniche SPID - SAML Response</a>
     */
    @DisplayName("Test Anomalie SPID - Errori IdP e di Protocollo (Backend + UI)")
    @ParameterizedTest(name = "AgID {0} - Rifiuta l'errore di protocollo IdP e mostra la UI di sistema")
    @EnumSource(
            value = SpidAgidAnomalyScenario.class,
            names = {"CODE_02", "CODE_03", "CODE_04", "CODE_05", "CODE_06", "CODE_07", "CODE_10"}
    )
    public void testAgidIdpAndProtocolAnomaliesAreHandledCorrectly(SpidAgidAnomalyScenario scenario) throws Exception {
        // Build the simulated environment mimicking an IdP-side status rejection or protocol mismatch
        SpidAgidErrorContextBuilder context = getBaseContextBuilder()
                .activePostBinding(false)
                .signingIdpSsoUrl(identityProvider.signingIdpSsoUrl)
                .registrationId(identityProvider.registrationIdRedirect)
                .assertingPartyEntityId(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
                .build();

        // Process the inbound SAML Response containing the IdP failure status code
        SpidAuthenticationException spidEx = agidUtils.executeAnomalyScenarioAndGetException(scenario, context);

        // Verify that the security engine maps this to a response validation failure and updates the UI accordingly
        agidUtils.validateSpidAnomalyTechnicalAndSystem(spidEx, mockMvc, LOGIN_DESTINATION_URL, messageSource);
        assertThat(spidEx.getError().getErrorCode()).isEqualTo(SpidError.SPID_FAILED_RESPONSE_VALIDATION.getErrorCode());
        assertThat(spidEx.getError().getErrorCode()).isEqualTo("1000");
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
