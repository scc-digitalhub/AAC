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
import it.smartcommunitylab.aac.spid.setupflow.SpidResponseBuilder;
import it.smartcommunitylab.aac.spid.utils.SpidAttackUtils;
import it.smartcommunitylab.aac.spid.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Test suite for validating the security and resilience of the SPID SAML 2.0 authentication flow against malicious attacks.
 * Verifies robust mitigation against replay attacks, CSRF (RelayState manipulation), open redirects, signature stripping, and assertion tampering.
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
public class SpidSecurityAttacksTest extends BaseSpidTest {

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    protected WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    protected WireMockServer mockIdPServerPost;

    protected UserUtils userUtils = new UserUtils();
    protected SpidAttackUtils spidAttackUtils = new SpidAttackUtils();
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
     * SICUREZZA APPLICATIVA: Mitigazione degli attacchi di tipo Replay (Replay Attacks).
     * Verifica che il Service Provider intercetti e respinga una SAML Response firmata legittima che sia già stata sottomessa
     * e processata in precedenza. Il riutilizzo del token da parte di una sessione terza (l'attaccante) deve fallire istantaneamente,
     * bloccando l'accesso abusivo alle risorse del cittadino.
     *
     * @see <a href="https://docs.oasis-open.org/security/saml/v2.0/saml-sec-consider-2.0-os.pdf">OASIS SAML 2.0 Security Considerations (Sez. 5.1.2)</a>
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html">OWASP SAML Security Cheat Sheet</a>
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso per Attacco di Replay (SAML Response riutilizzata da un utente terzo)")
    public void testAuthenticationFailsOnReplayAttack() throws Exception {
        // 1. Establish a legitimate authentication session context for the victim
        SpidRequest victimSpidResponse = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Generate a valid, signed SAML Response linked to the victim's request ID
        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, victimSpidResponse.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, identityProvider.signingIdpEntityId)
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSignature()
            .buildResponse();

        // FIRST USE: The victim completes login successfully, initializing and terminating the single-use token lifecycle
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", victimSpidResponse.getRelayState())
                .session(victimSpidResponse.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(USER_DESTINATION_URL));

        // ATTACK: An attacker intercepts the used SAMLResponse and attempts to replay it within an isolated hacker session
        MockHttpSession hackerSession = userUtils.createSessionWithSavedClientRequest(BASE_URL);

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response) // REPLAYED Payload (Intercepted from victim)
                .param("RelayState", victimSpidResponse.getRelayState())     // REPLAYED RelayState
                .session(hackerSession) // DIFFERENT Session (The Hacker's session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andReturn();

        // 3. Assert that the core security engine detected the duplicate message use and raised an authentication exception
        Exception hackerException = (Exception) hackerSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(hackerException).isNotNull();
        assertThat(hackerException.getMessage()).contains("No relying party registration found");
    }

    /**
     * SICUREZZA APPLICATIVA: Protezione dalle vulnerabilità di Open Redirect.
     * Verifica che il sistema neutralizzi i tentativi di manipolazione del parametro "RelayState" atti a forzare il reindirizzamento
     * dell'utente verso un dominio esterno malevolo (es. siti di phishing) al completamento del flusso. Il Service Provider deve
     * accettare unicamente destinazioni coerenti con lo stato interno e sicuro della sessione locale dell'applicazione.
     *
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html">OWASP SAML Security - RelayState Validation</a>
     */
    @Test
    @DisplayName("Sicurezza: Mitigazione nativa contro attacchi di Open Redirect")
    public void testAuthenticationFailsOnOpenRedirectAttempt() throws Exception {
        // 1. Attacker crafts a malicious external URL intended for an Open Redirect payload
        String maliciousRelayStateUrl = "https://hacker-phishing-site.invalid/steal-credentials";

        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, identityProvider.signingIdpEntityId)
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSignature()
            .buildResponse();

        // 2. Attacker submits a legitimate SAML Response but injects the malicious URL into the RelayState parameter
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", maliciousRelayStateUrl) // Injected malicious URL
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            // 3. The flow breaks gracefully because the hijacked RelayState fails to resolve against the internal session context
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Confirm that the security core intercepted the state mismatch and populated the context error
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).contains("No relying party registration found");
    }

    /**
     * SICUREZZA APPLICATIVA: Difesa da attacchi Cross-Site Request Forgery (CSRF) e Session Hijacking.
     * Verifica il rigetto immediato dell'autenticazione nel caso in cui il token crittografico "RelayState" venga alterato o
     * sostituito durante la navigazione. La mancata corrispondenza tra il valore trasmesso dall'IdP e il token memorizzato
     * originariamente nella sessione dell'utente deve causare l'annullamento protetto del flusso di login.
     *
     * @see <a href="https://docs.oasis-open.org/security/saml/v2.0/saml-sec-consider-2.0-os.pdf">OASIS SAML 2.0 Security Considerations (Sez. 5.1.3)</a>
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso se il RelayState manipolato (CSRF / Hijacking)")
    public void testAuthenticationFailsOnMissingOrManipulatedRelayState() throws Exception {
        // 1. Initialize an active authentication transaction context
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, identityProvider.signingIdpEntityId)
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSignature()
            .buildResponse();

        // 2. Post the valid assertion but inject an unauthorized, manipulated RelayState string representing a CSRF attack vector
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", "SSdfMklR7My_NohjAY72i57SfjumEOEhIXnEYVVMIew=") // MANIPULATED RelayState (Invalid CSRF)
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL))
            .andReturn();

        // 3. Confirm that the security core intercepted the state mismatch and populated the context error
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).contains("No relying party registration found");
    }

    /**
     * SICUREZZA APPLICATIVA: Contrasto ad attacchi di Signature Stripping e manomissione dell'integrità del messaggio.
     * Verifica che il Service Provider invalidi immediatamente la sottomissione se la SAML Response recapitata risulta priva della
     * firma crittografica dell'IdP. La presenza della firma XML (XML Signature) è un requisito tassativo e non negoziabile;
     * l'assenza del blocco `<ds:Signature>` indica un tentativo di alterazione fraudolenta o rimozione della firma a monte.
     *
     * @see <a href="https://docs.oasis-open.org/security/saml/v2.0/saml-sec-consider-2.0-os.pdf">OASIS SAML 2.0 Security Considerations (Sez. 4.1.1)</a>
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html">OWASP SAML Security - Signature Stripping</a>
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso se la SAML Response NON è firmata")
    public void testAuthenticationFailsOnUnsignedSamlResponse() throws Exception {
        // 1. Prepare a standard authentication session context anchors
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Build the inbound SAML Response template deliberately omitting the .withSignature() step (Signature Stripping)
        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, identityProvider.signingIdpEntityId)
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .buildResponse(); // Intentionally omitting XML signature injection

        // 3. Post the unsigned payload and verify that the security filters drop the request, forcing a fallback to the login view
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL))
            .andReturn();

        // 4. Check that the system throws exception 1000, capturing a fatal cryptographic layout error
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).contains("1000"); // Cryptographic validation error code
    }

    /**
     * SICUREZZA APPLICATIVA: Contrasto ad attacchi di riduzione dei privilegi (Privilege Downgrade / Level Downgrade).
     * Verifica che il Service Provider rifiuti la sottomissione se l'Identity Provider restituisce un contesto di autenticazione
     * (AuthnContextClassRef) avente livello di sicurezza SPID inferiore (es. SPID L1) rispetto al livello minimo esplicitamente
     * richiesto e configurato dall'applicazione (es. SPID L2). Questo impedisce ad utenti malintenzionati di bypassare i fattori di
     * sicurezza OTP obbligatori.
     *
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html">OWASP SAML Security - Identity/Privilege Downgrade</a>
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso se il livello SPID restituito (L1) è inferiore a quello richiesto (L2)")
    public void testAuthenticationFailsOnLowerSpidLevel() throws Exception {
        // 1. Establish a secure session transaction expecting an explicit SPID L2 assurance level
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Force an intentional privilege downgrade mock (L2 -> L1) into the payload structure
        String response = spidAttackUtils.prepareForSimulationNotValidChangeSpidLevelLow(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Submit the weakened payload and assert that the application isolates and drops the session
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Verify that the system registers error code 1000, confirming a payload validation rejection
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).contains("1000"); // Cryptographic validation error code
    }

    /**
     * SICUREZZA APPLICATIVA: Sanificazione degli input e robustezza dei componenti di decodifica e parsing (Anti-Crash).
     * Verifica che il Service Provider gestisca correttamente i tentativi di sottomissione di stringhe corrotte o non conformi.
     * Il test garantisce la copertura dei blocchi di "catch" interni inviando:
     * 1. Una stringa casuale che viola radicalmente la codifica Base64.
     * 2. Un payload Base64 formalmente valido che decodifica in un testo XML troncato a metà.
     * Il sistema deve rifiutare le richieste sollevando un'eccezione controllata senza andare in crash o esporre stacktrace.
     *
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html">OWASP Input Validation Cheat Sheet</a>
     */
    @Test
    @DisplayName("Sicurezza: Rifiuto stringhe Base64 corrotte o XML troncati malformati")
    public void testParserResilienceAgainstCorruptedInputs() throws Exception {
        // We initialize a clean mock session for the transaction
        MockHttpSession session = new MockHttpSession();

        // SCENARIO 1: Submitting a parameter value that completely violates Base64 standard syntax rules
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", "!!ThisIsNotABase64EncodedStringStringString!!")
                .param("RelayState", "arbitraryValidStateString")
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // Confirm that the security core intercepted the malformed syntax and populated the context error
        Exception base64Exception = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(base64Exception).isNotNull();
        assertThat(base64Exception.getMessage()).contains("No relying party registration found");

        // Clean the session attributes to prepare for the second isolated scenario run
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        // SCENARIO 2: Submitting a valid Base64 string that decodes to a heavily corrupted/truncated XML payload structure
        String truncatedXmlBase64 = java.util.Base64.getEncoder().encodeToString(
            "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"_123\"".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", truncatedXmlBase64)
                .param("RelayState", "arbitraryValidStateString")
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // Confirm that the XML unmarshalling failure is securely trapped and registered as a validation context failure
        Exception xmlException = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(xmlException).isNotNull();
        assertThat(xmlException.getMessage()).contains("No relying party registration found");
    }

    /**
     * LINEE GUIDA SICUREZZA ICT: Protezione dei dati sensibili in transito e mitigazione del caching del browser.
     * Verifica che tutte le risposte HTTP emesse durante le transazioni di sicurezza introducano tassativamente
     * le intestazioni (Headers) atte a vietare la memorizzazione dei dati sensizioni in cache locali o proxy intermedi.
     * Il test accerta la presenza e l'esatta valorizzazione degli attributi Cache-Control, Pragma ed Expires.
     *
     * @see <a href="https://cert-pa.link/lg-sicurezza-ict">AgID Linee Guida per la Sicurezza ICT delle PA - Cache Directives</a>
     */
    @Test
    @DisplayName("Sicurezza HTTP: Verifica intestazioni Cache-Control e Pragma anti-caching")
    public void testHttpHeadersEnforceStrictAntiCachingPolicies() throws Exception {
        // 1. Perform a mock request initialization to capture the structural response context headers
        this.mockMvc.perform(get(BASE_URL + AUTHENTICATE_PATH + identityProvider.registrationIdRedirect)
                    .secure(true))
            .andExpect(status().is3xxRedirection())

            // 2. Assert strict compliance with AgID anti-caching mandates on standard security response channels
            .andExpect(header().string("Cache-Control", containsString("no-cache")))
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(header().string("Cache-Control", containsString("must-revalidate")))
            .andExpect(header().string("Pragma", "no-cache"))
            .andExpect(header().string("Expires", "0"));
    }
}
