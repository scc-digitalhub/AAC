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
import it.smartcommunitylab.aac.spid.utils.SpidAuthPositiveUtils;
import it.smartcommunitylab.aac.spid.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
public class SpidAuthResponsePositivePathTest extends BaseSpidTest {

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    protected WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    protected WireMockServer mockIdPServerPost;

    protected UserUtils userUtils = new UserUtils();
    protected SpidAuthPositiveUtils spidAuthPositiveUtils = new SpidAuthPositiveUtils();
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

    @Test
    @DisplayName("Autenticazione con successo: HTTP-Redirect Binding")
    public void testAuthenticationSucceedsWithRedirectBinding() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect) // REDIRECT
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, identityProvider.signingIdpEntityId) // REDIRECT
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSignature()
            .buildResponse();

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(USER_DESTINATION_URL));
    }

    @Test
    @DisplayName("Autenticazione con successo: HTTP-POST Binding")
    public void testAuthenticationSucceedsWithPostBinding() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdPost) // POST
            .withPostBinding(true)
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST, identityProvider.signingIdpEntityId) // POST
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSignature()
            .buildResponse();

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
     * Security Test: Ensures authentication succeed if the Identity Provider
     * returns a SPID security level higher than the one requested.
     */
    @Test
    @DisplayName("Autenticazione con successo: livello SPID restituito (L3) è superiore a quello richiesto (L2)")
    public void testAuthenticationSucceedOnHighSpidLevel() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Legitimate upgrade SPID level (L2 -> L3) via HackerUtils (using it as a generic utility here)
        // Or via normal builder if preferred. Using the existing implementation:
        String response = spidAuthPositiveUtils.prepareForSimulationNotValidChangeSpidLevelHigh(
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
            .andExpect(redirectedUrl(USER_DESTINATION_URL));
    }

    @Test
    @DisplayName("Verifica runtime HTML Form (HTTP-POST)")
    public void testRuntimeHtmlFormStructurePost() throws Exception {
        // 1. Create an active session pre-populated with a protected resource request
        // This ensures Spring Security will automatically generate a valid RelayState
        MockHttpSession session = userUtils.createSessionWithSavedClientRequest(BASE_URL);

        // 2. Execute the SSO initialization request directly and capture the raw HTML response
        String htmlResponse = mockMvc.perform(post(BASE_URL + AUTHENTICATE_PATH + identityProvider.registrationIdPost)
                .secure(true)
                .session(session))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // 3. Verify the HTML response contains a form intended for the IdP via POST
        assertThat(htmlResponse).contains("<form");
        assertThat(htmlResponse).containsIgnoringCase("method=\"post\"");

        String action = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
            .getRelyingPartyRegistration(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST)
            .getAssertingPartyDetails()
            .getSingleSignOnServiceLocation();

        assertThat(htmlResponse).contains("action=\"" + action + "\"");

        // 4. Verify the presence of the mandatory hidden SAML inputs
        assertThat(htmlResponse).contains("name=\"SAMLRequest\"");
        assertThat(htmlResponse).contains("name=\"RelayState\"");
    }
}
