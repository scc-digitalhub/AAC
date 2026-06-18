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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
     * Security Test: Verifies protection against Replay Attacks.
     * The system must reject a SAML Response that has already been processed.
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso per Attacco di Replay (SAML Response riutilizzata da un utente terzo)")
    public void testAuthenticationFailsOnReplayAttack() throws Exception {
        SpidRequest victimSpidResponse = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, victimSpidResponse.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, identityProvider.signingIdpEntityId)
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSignature()
            .buildResponse();

        // FIRST USE (Victim completes login successfully)
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", victimSpidResponse.getRelayState())
                .session(victimSpidResponse.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(USER_DESTINATION_URL));

        // ATTACK: attacker intercepts the signed SAMLResponse and attempts to replay it.
        MockHttpSession hackerSession = userUtils.createSessionWithSavedClientRequest(BASE_URL);

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response) // REPLAYED Payload (Intercepted from victim)
                .param("RelayState", victimSpidResponse.getRelayState())     // REPLAYED RelayState
                .session(hackerSession) // DIFFERENT Session (The Hacker's session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andReturn();

        Exception hackerException = (Exception) hackerSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(hackerException).isNotNull();
        assertThat(hackerException.getMessage()).contains("No relying party registration found");
    }

    /**
     * Security Concept: Open Redirect Vulnerability Mitigation.
     */
    @Test
    @DisplayName("Sicurezza: Mitigazione nativa contro attacchi di Open Redirect")
    public void testAuthenticationFailsOnOpenRedirectAttempt() throws Exception {
        // 1. Attacker crafts a malicious URL intended for an Open Redirect payload
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

        // 2. Attacker submits a legitimate SAML Response but injects the malicious URL into the RelayState
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", maliciousRelayStateUrl) // Injected malicious URL
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            // 3. Flow breaks gracefully because the RelayState does not match the internal session context
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));
    }

    /**
     * Security Test: Ensures authentication fails if the RelayState parameter
     * is missing, unexpected, or has been manipulated during the flow.
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso se il RelayState manipolato (CSRF / Hijacking)")
    public void testAuthenticationFailsOnMissingOrManipulatedRelayState() throws Exception {
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

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", "SSdfMklR7My_NohjAY72i57SfjumEOEhIXnEYVVMIew=") // MANIPULATED RelayState (Invalid CSRF)
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL))
            .andReturn();

        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).contains("No relying party registration found");
    }

    /**
     * Security Test: Ensures authentication fails if the Identity Provider
     * returns a SPID security level lower than the one requested.
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso se il livello SPID restituito (L1) è inferiore a quello richiesto (L2)")
    public void testAuthenticationFailsOnLowerSpidLevel() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Downgrade Livello SPID (L2 -> L1) - Privilege Downgrade Attack
        String response = spidAttackUtils.prepareForSimulationNotValidChangeSpidLevelLow(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // Verify error is 1000 (errore di validazione del payload)
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).contains("1000");
    }

    /**
     * Security Test: Ensures authentication fails and the payload is rejected
     * if the SAML Response lacks a valid cryptographic signature.
     */
    @Test
    @DisplayName("Sicurezza: Fallimento atteso se la SAML Response NON è firmata")
    public void testAuthenticationFailsOnUnsignedSamlResponse() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, identityProvider.signingIdpEntityId)
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .buildResponse(); // Intentionally omitting .withSignature()

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response) // UNSIGNED Payload (Rejected by SP)
                .param("RelayState", spidRequest.getRelayState())     // Valid State
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL))
            .andReturn();

        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).contains("1000"); // Cryptographic validation error code
    }
}
