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
import it.smartcommunitylab.aac.spid.utils.SpidAuthnPositiveUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for validating the standard SPID SAML 2.0 authentication flows and structures.
 * Verifies the happy paths, binding structures, and legitimate protocol behaviors (e.g. Spid Level upgrades).
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
public class SpidAuthnResponsePositivePathTest extends BaseSpidTest {

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    protected WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    protected WireMockServer mockIdPServerPost;

    protected SpidAuthnPositiveUtils spidAuthnPositiveUtils = new SpidAuthnPositiveUtils();
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
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)".
     * Verifica il completamento con successo dell'autenticazione quando la richiesta originaria è stata inviata tramite HTTP-Redirect.
     * Il test simula la ricezione del modulo di risposta firmato (SAMLResponse) inviato dall'IdP all'endpoint di ricezione (ACS)
     * del Service Provider, validando lo stato "Success", verificando i certificati e accertando il corretto reindirizzamento
     * finale dell'utente verso la risorsa protetta richiesta originariamente.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Autenticazione con successo: HTTP-Redirect Binding")
    public void testAuthenticationSucceedsWithRedirectBinding() throws Exception {
        // 1. Initialize the outbound flow using the HTTP-Redirect configuration profile
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect) // REDIRECT
            .withSession()
            .executeRequest();

        // 2. Mock a fully valid and cryptographically signed SAML Response matching the outbound request ID
        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, identityProvider.signingIdpEntityId) // REDIRECT
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSignature()
            .buildResponse();

        // 3. Post the SAMLResponse back to the ACS endpoint and verify security filters finalize session creation
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(USER_DESTINATION_URL));
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Ricezione delle risposte (SAML Response)".
     * Verifica il completamento con successo dell'autenticazione quando la richiesta originaria è stata trasmessa tramite HTTP-POST.
     * Garantisce l'interoperabilità del backend nell'accettare e validare risposte collegate a flussi POST asincroni,
     * confrontando correttamente l'ID della richiesta memorizzato nella sessione dell'utente con l'attributo InResponseTo
     * presente nell'asserzione SAML ricevuta.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Autenticazione con successo: HTTP-POST Binding")
    public void testAuthenticationSucceedsWithPostBinding() throws Exception {
        // 1. Initialize the outbound flow enforcing the HTTP-POST request binding profile
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdPost) // POST
            .withPostBinding(true)
            .withSession()
            .executeRequest();

        // 2. Build a valid, signed SAML Response targeting the POST-binding configurations
        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST, identityProvider.signingIdpEntityId) // POST
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSignature()
            .buildResponse();

        // 3. Dispatch the response payload to the local ACS listener and evaluate successful authentication
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(USER_DESTINATION_URL));
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)" -> Gestione del livello richiesto.
     * Test di Sicurezza e Conformità: Verifica che il Service Provider accetti con successo l'autenticazione se l'Identity
     * Provider restituisce un livello di sicurezza SPID (es. SPID L3) superiore a quello minimo inizialmente richiesto (es. SPID L2).
     * Questo comportamento è regolato dall'attributo Comparison="minimum" configurato nel RequestedAuthnContext. Il test assicura
     * che il motore di sicurezza non consideri l'innalzamento di livello un'anomalia di protocollo, convalidando la sessione
     * dell'utente a livello ottimale.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - Ricezione risposte SAML</a>
     */
    @Test
    @DisplayName("Autenticazione con successo: livello SPID restituito (L3) è superiore a quello richiesto (L2)")
    public void testAuthenticationSucceedOnHighSpidLevel() throws Exception {
        // 1. Trigger a standard authentication request flow expecting a minimum of SPID L2
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // 2. Generate an upgraded SAML Response containing a higher assurance profile (SPID L3) than requested
        String response = spidAuthnPositiveUtils.prepareForSimulationNotValidChangeSpidLevelHigh(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        // 3. Post the upgraded assertion to ensure the framework accommodates the security context elevator natively
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(USER_DESTINATION_URL));
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Single Sign-On" -> "Response".
     * Verifica la corretta applicazione della tolleranza al disallineamento temporale (Clock Skew) entro i limiti fisiologici ammessi.
     * In conformità con i vincoli di ricezione ed elaborazione normati da AgID nella sezione "Response", il Service Provider
     * esegue il controllo sui timestamp del token. Seguendo le raccomandazioni dello standard SAML Core 2.0, viene applicata
     * una finestra di tolleranza (Clock Skew) per compensare minime latenze di trasmissione o sfasamenti tra i server.
     * Il test garantisce che una risposta emessa in anticipo di 30 secondi nel futuro rispetto all'orologio locale venga
     * accettata dall'infrastruttura, finalizzando l'autenticazione senza sollevare eccezioni.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response">Regole Tecniche SPID - SSO Response</a>
     * @see <a href="https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf">OASIS SAML Core 2.0 Standard (Sez. 2.5.1.1 - Clock Skew)</a>
     */
    @Test
    @DisplayName("Autenticazione con successo: Validazione limiti di Clock Skew temporale +30s")
    public void testAuthenticationSucceedClockSkewToleranceThresholds() throws Exception {
        // Premature token but within acceptable clock skew tolerance (+30 seconds)
        SpidRequest spidRequestValid = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String validSkewResponse = spidAuthnPositiveUtils.prepareResponseWithTime(
            spidRequestValid,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", validSkewResponse)
                .param("RelayState", spidRequestValid.getRelayState())
                .session(spidRequestValid.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(USER_DESTINATION_URL)); // Should pass successfully within tolerance
    }
}
