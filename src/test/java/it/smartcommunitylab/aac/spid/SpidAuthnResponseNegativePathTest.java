package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockIdpSpid;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.utils.SpidAuthnNegativeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for validating the SAML protocol compliance of the Service Provider.
 * Verifies that the SP correctly filters out malformed, incorrectly signed, unsolicited, or expired SAML Responses.
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
public class SpidAuthnResponseNegativePathTest extends BaseSpidTest {

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    protected WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    protected WireMockServer mockIdPServerPost;

    protected SpidAuthnNegativeUtils spidAuthnNegativeUtils = new SpidAuthnNegativeUtils();
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
                ConfigurableIdentityProvider idp = idps.get(3);

                identityProvider.initRealmByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);
                identityProvider.initRegistrationIdBinding(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)" -> Controllo della sessione.
     * Test di Sicurezza: Verifica che l'autenticazione fallisca se l'attributo "InResponseTo" presente nella SAML Response
     * non corrisponde all'ID univoco della AuthnRequest originariamente generata e memorizzata in sessione.
     * Questo controllo impedisce attacchi di inserimento di sessione o tentativi di tracciamento non autorizzati, garantendo
     * il legame biunivoco e transazionale tra la richiesta del Service Provider e la risposta dell'IdP.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per InResponseTo errato (Risposta non corrispondente alla richiesta)")
    public void testAuthenticationFailsOnInvalidInResponseTo() throws Exception {
        // 1. Initialize a legitimate outbound authentication request flow
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Mock an inbound payload containing a mismatched Request ID inside the InResponseTo attribute
        String response = spidAuthnNegativeUtils.prepareForSimulationNotValidRequestId(
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Dispatch the corrupted payload and verify the SP aborts login and redirects to the error landing page
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Assert that the framework intercepted the destination routing mismatch securely
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("does not match the ID of the authentication request");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)" -> Divieto di Unsolicited Response.
     * Test di Sicurezza: Verifica che il Service Provider rifiuti tassativamente le risposte di tipo "Unsolicited"
     * (ovvero prive dell'attributo InResponseTo).
     * AgID vieta categoricamente i flussi "IdP-Initiated SSO". L'autenticazione deve essere sempre avviata esplicitamente
     * dall'utente sul Service Provider; risposte prive del tracciamento transazionale devono essere immediatamente scartate.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Unsolicited Response (InResponseTo assente)")
    public void testAuthenticationFailsOnUnsolicitedResponse() throws Exception {
        // 1. Trigger the standard outbound flow to establish a stateful session context
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Generate a signed SAML response completely missing the mandatory InResponseTo attribute
        String response = spidAuthnNegativeUtils.prepareForSimulationUnsolicitedResponse(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Post the unsolicited token and verify that the security engine blocks authentication
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Assert that the underlying framework correctly populated the session with a dedicated security exception
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("1000");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)" -> Cerchia di fiducia (Trust).
     * Test di Sicurezza: Verifica che l'autenticazione venga bloccata se l'elemento `<saml2:Issuer>` all'interno della risposta
     * non corrisponde a nessuno degli EntityID degli Identity Provider censiti ed autorizzati nel registro del Service Provider.
     * Questo meccanismo previene attacchi di impersonificazione o tentativi di sottomissione di asserzioni da parte di entità malevole esterne.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Issuer Mismatch (EntityID IdP non registrato)")
    public void testAuthenticationFailsOnIssuerMismatch() throws Exception {
        // 1. Establish the target user authentication session via standard dispatch
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Build a cryptographically valid response payload but signed by an unknown/unregistered entity ID
        String response = spidAuthnNegativeUtils.prepareForSimulationIssuerMismatch(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Deliver the foreign assertion to the endpoint and verify rejection
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Check that the Spring Security core captured the unauthorized trust circle mismatch
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("Invalid signature for object [_response_test_id_value]");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)" -> Condizioni temporali.
     * Test di Sicurezza: Verifica che il Service Provider rifiuti la risorsa se l'asserzione risulta scaduta nel tempo,
     * ovvero se il timestamp corrente supera il limite massimo definito dall'attributo "NotOnOrAfter" nelle Conditions.
     * Questa validazione previene attacchi di tipo replay (riutilizzo di vecchi token validi intercettati).
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Assertion scaduta")
    public void testAuthenticationFailsOnExpiredAssertion() throws Exception {
        // 1. Trigger the standard outbound flow to anchor session states
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Generate an outdated SAML response where the NotOnOrAfter condition is set to a historical past date
        String response = spidAuthnNegativeUtils.prepareForSimulationNotValidNotOnOrAfter(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Post the stale token and confirm that routing redirects back to the login page due to access denial
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Verify that Spring Security explicitly rejected the Assertion due to time restrictions (stale check)
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("is no longer valid");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)" -> Condizioni temporali.
     * Test di Sicurezza: Verifica che il Service Provider rifiuti la risorsa se l'asserzione non è ancora valida,
     * ovvero se l'orario corrente precede il timestamp impostato nell'attributo "NotBefore" delle Conditions.
     * Questo controllo assicura il rispetto rigoroso delle finestre di validità temporale concordate nella federazione.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per NotBefore futuro (Assertion non ancora valida)")
    public void testAuthenticationFailsOnNotBeforeFuture() throws Exception {
        // 1. Build a normal outbound context session
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Generates a response token where the Assertion's validity window begins in the future
        String response = spidAuthnNegativeUtils.prepareForSimulationNotValidNotBeforeFuture(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Attempt transmission and ensure the filter chain breaks the request routing
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Verifies that Spring Security has raised a time-related premature authentication exception
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("is not yet valid");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)" -> Restrizione dell'audience.
     * Test di Sicurezza: Verifica che l'autenticazione fallisca se l'elemento `<saml2:Audience>` all'interno della risposta
     * contiene un valore differente dall'EntityID del Service Provider ricevente.
     * Questa validazione impedisce attacchi di tipo "Token Substitution", bloccando lo scenario in cui un'asserzione legittima,
     * emessa per un determinato Service Provider ("SP-A"), venga intercettata e riutilizzata per accedere abusivamente a un altro ("SP-B").
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Audience errata (Risposta destinata a un altro Service Provider)")
    public void testAuthenticationFailsOnInvalidAudience() throws Exception {
        // 1. Establish the current active transactional session state
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Build a response token targeting a fictitious, invalid SP EntityID inside the Audience Restrictions block
        String response = spidAuthnNegativeUtils.prepareForSimulationNotValidEntityId(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Deliver the mismatched audience payload and verify immediate login rejection
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Assert that the framework intercepted the destination routing mismatch securely
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("Invalid assertion [_assertion_test_id_value] for SAML response [_response_test_id_value]: Condition '{urn:oasis:names:tc:SAML:2.0:assertion}AudienceRestriction' of type 'null' in assertion '_assertion_test_id_value' was not valid.: None of the audiences within Assertion '_assertion_test_id_value' matched the list of valid audiances");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)" -> Validazione dell'endpoint di ricezione (Recipient).
     * Test di Sicurezza: Verifica che la sottomissione fallisca se l'attributo "Recipient" nell'elemento SubjectConfirmationData
     * o l'attributo "Destination" della risposta non coincidono esattamente con l'URL dell'endpoint ACS corrente del Service Provider.
     * Questo blocco garantisce che la risposta sia recapitata esattamente all'interfaccia di destinazione prevista e configurata nel flusso di trust.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Recipient Mismatch (Destination errato)")
    public void testAuthenticationFailsOnRecipientMismatch() throws Exception {
        // 1. Trigger the standard outbound framework request to create context anchors
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Generates an inbound response payload containing an incorrect, fictitious Destination endpoint location URI
        String response = spidAuthnNegativeUtils.prepareForSimulationRecipientMismatch(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Post the payload and evaluate proper rejection with redirection to the base login page
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Assert that the framework intercepted the destination routing mismatch securely
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("Invalid destination [https://hacker-endpoint.invalid/auth/spid/sso/malicious] for SAML response [_response_test_id_value]");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Single Sign-On" -> "Response".
     * Verifica il rigetto tassativo di un'asserzione SAML il cui timestamp di generazione risulta fuori dalle soglie di tolleranza ammesse.
     * In conformità con i vincoli di convalida degli attributi temporali (IssueInstant, NotBefore) definiti da AgID nella sezione "Response",
     * il Service Provider deve scartare i messaggi che superano il disallineamento massimo consentito dallo standard internazionale SAML Core.
     * Il test accerta che il motore di sicurezza intercetti la violazione temporale di 90 secondi (oltre la soglia di Clock Skew),
     * neghi l'accesso respingendo il login e popoli la sessione con la relativa eccezione di sicurezza del framework.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - SSO Response</a>
     * @see <a href="https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf">OASIS SAML Core 2.0 Standard (Sez. 2.5.1.1 - Clock Skew)</a>
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per limiti di Clock Skew temporale +90s")
    public void testAuthenticationFailsOutsideClockSkewToleranceThresholds() throws Exception {
        // Premature token well outside acceptable clock skew limits (+90 seconds) ---
        SpidRequest spidRequestInvalid = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String invalidSkewResponse = spidAuthnNegativeUtils.prepareResponseWithTimeTooOld(
            spidRequestInvalid,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", invalidSkewResponse)
                .param("RelayState", spidRequestInvalid.getRelayState())
                .session(spidRequestInvalid.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL)); // Must be blocked and rejected

        // Confirm that the context captured the security violation in session
        Exception sessionException = (Exception) spidRequestInvalid.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("assertion IssueInstant is after the instant of the received response");
    }
}
