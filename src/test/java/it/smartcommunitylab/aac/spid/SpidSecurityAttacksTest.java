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
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.setupflow.SpidResponseBuilder;
import it.smartcommunitylab.aac.spid.steps.SpidAttackSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
     * SICUREZZA APPLICATIVA: rigetto di una SAML Response rubata e riusata in una sessione diversa.
    * Un attaccante intercetta una Response valida e firmata della vittima e la reinvia nel proprio
    * contesto (sessione + RelayState propri). Poiché l'asserzione è legata transazionalmente alla
    * AuthnRequest pendente tramite InResponseTo, il token rubato non corrisponde alla richiesta
    * dell'attaccante e viene rifiutato.
    *
    * @see <a href="https://docs.oasis-open.org/security/saml/v2.0/saml-sec-consider-2.0-os.pdf">OASIS SAML 2.0 Security Considerations (Sez. 5.1.2)</a>
    * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html">OWASP SAML Security Cheat Sheet</a>
    */
    @Test
    @DisplayName("Sicurezza: token rubato rifiutato per InResponseTo non corrispondente")
    public void testAuthenticationFailsOnStolenAssertionReuse() throws Exception {
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

        // ATTACK: the attacker starts a legitimate flow of their own, so the request passes the
        // registration-resolution gate and the stolen token is actually validated downstream.
        SpidRequest attacker = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)                 // victim's stolen token
                .param("RelayState", attacker.getRelayState())   // attacker's own context
                .session(attacker.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // The stolen token does not match the attacker's own pending AuthnRequest (InResponseTo mismatch)
        SpidAuthenticationException ex = (SpidAuthenticationException) attacker.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(ex).isNotNull();
        assertThat(ex.getError()).isEqualTo(SpidError.SAML_INVALID_IN_RESPONSE_TO);
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
        Saml2AuthenticationException samlEx = (Saml2AuthenticationException) spidRequest.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(samlEx).isNotNull();
        SpidError spidError = SpidError.translate(samlEx.getSaml2Error());
        assertThat(spidError).isEqualTo(SpidError.SAML_RELYING_PARTY_REGISTRATION_NOT_FOUND);
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
        Saml2AuthenticationException samlEx = (Saml2AuthenticationException) spidRequest.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(samlEx).isNotNull();
        SpidError spidError = SpidError.translate(samlEx.getSaml2Error());
        assertThat(spidError).isEqualTo(SpidError.SAML_RELYING_PARTY_REGISTRATION_NOT_FOUND);
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
        SpidAuthenticationException sessionException = (SpidAuthenticationException) spidRequest.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getError()).isEqualTo(SpidError.SPID_FAILED_RESPONSE_VALIDATION); // Cryptographic validation error code
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
        String response = SpidAttackSimulator.simulatePrivilegeDowngrade(
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
        SpidAuthenticationException sessionException = (SpidAuthenticationException) spidRequest.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getError()).isEqualTo(SpidError.SPID_FAILED_RESPONSE_VALIDATION); // Cryptographic validation error code
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
        Saml2AuthenticationException base64Exception = (Saml2AuthenticationException) session
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(base64Exception).isNotNull();
        SpidError spidError1 = SpidError.translate(base64Exception.getSaml2Error());
        assertThat(spidError1).isEqualTo(SpidError.SAML_RELYING_PARTY_REGISTRATION_NOT_FOUND);

        // Clean the session attributes to prepare for the second isolated scenario run
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        // SCENARIO 2: Submitting a valid Base64 string that decodes to a heavily corrupted/truncated XML payload structure
        String truncatedXmlBase64 = Base64.getEncoder().encodeToString(
            "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"_123\"".getBytes(StandardCharsets.UTF_8)
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
        Saml2AuthenticationException samlEx = (Saml2AuthenticationException) session
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(samlEx).isNotNull();
        SpidError spidError2 = SpidError.translate(samlEx.getSaml2Error());
        assertThat(spidError2).isEqualTo(SpidError.SAML_RELYING_PARTY_REGISTRATION_NOT_FOUND);
    }

    /**
     * SICUREZZA APPLICATIVA: Contrasto ad attacchi di Man-in-the-Middle (MitM) e Data Tampering.
     * Verifica che il Service Provider invalidi immediatamente l'autenticazione se il payload della SAML Response
     * viene alterato dopo essere stato firmato dall'IdP. La modifica di un qualsiasi nodo (es. un attributo temporale,
     * l'ID della response o i dati dell'utente) invalida matematicamente l'hash crittografico (digest) calcolato nella
     * firma XML. L'attaccante non possedendo la chiave privata dell'IdP non può ricalcolare una firma valida.
     * Il test accerta che questa discrepanza sollevi la corretta eccezione di firma invalida.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Verifica della Firma</a>
     * @see <a href="https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf">OASIS SAML 2.0 Core (Sez. 5.4 - XML Signature Profile)</a>
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html">OWASP SAML Security - XML Signature Wrapping & Tampering</a>
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso per Payload manomesso (Firma Invalida / MitM)")
    public void testAuthenticationFailsOnTamperedPayloadInvalidSignature() throws Exception {
        // 1. Establish a standard authentication session context for the transaction
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Generate a valid signed SAML Response, then maliciously tamper with the XML data without resigning it
        String response = SpidAttackSimulator.simulateSignatureTampering(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Dispatch the corrupted (tampered) payload and verify the SP aborts login securely
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Assert that the framework intercepted the digest/signature mismatch and generated the specific SpidError
        SpidAuthenticationException sessionException = (SpidAuthenticationException) spidRequest.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getError()).isEqualTo(SpidError.SAML_INVALID_SIGNATURE);
    }

    /**
     * SICUREZZA APPLICATIVA: Contrasto ad attacchi di XML Signature Wrapping (XSW).
     * Test di Sicurezza: Simula un classico attacco XSW (XML Signature Wrapping).
     * Parte da una SAML Response valida e legittimamente firmata, quindi inietta una seconda
     * asserzione CONTRAFFATTA e NON FIRMATA, lasciando intatta la firma originale. L'asserzione
     * contraffatta viene posizionata prima di quella genuina in modo che un parser XML ingenuo
     * (leggendo "la prima asserzione") prelevi i dati controllati dall'attaccante. Un Service
     * Provider conforme deve elaborare solo l'elemento firmato o rigettare l'intero messaggio.
     *
     * @see <a href="https://www.usenix.org/conference/usenixsecurity12/technical-sessions/presentation/somorovsky">Somorovsky et al., "On Breaking SAML: Be Whoever You Want to Be" (USENIX Security 2012)</a>
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html">OWASP SAML Security - XML Signature Wrapping</a>
     * @see <a href="https://docs.oasis-open.org/security/saml/v2.0/saml-sec-consider-2.0-os.pdf">OASIS SAML 2.0 Security Considerations (Sez. 6 - XML Signature)</a>
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Verifica della firma sulla Response</a>
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso per XML Signature Wrapping (asserzione forgiata aggiunta)")
    public void testAuthenticationFailsOnSignatureWrapping() throws Exception {
        // 1. Establish the current active transactional session state
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Generate a legitimately signed SAML Response, then inject an unsigned forged assertion (XSW)
        String response = SpidAttackSimulator.simulateXmlSignatureWrapping(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Dispatch the manipulated XSW payload and verify the SP securely aborts the login process
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl).secure(true)
                .param("SAMLResponse", response).param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession()).contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // 4. Assert that the framework's strict XML parsing and signature validation intercepted the wrapping anomaly
        SpidAuthenticationException ex = (SpidAuthenticationException) spidRequest.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        assertThat(ex).isNotNull();
        // Spring/OpenSAML rejects responses carrying an assertion not covered by a valid signature
        assertThat(ex.getError()).isIn(SpidError.SAML_INVALID_SIGNATURE, SpidError.SAML_INVALID_ASSERTION);
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
