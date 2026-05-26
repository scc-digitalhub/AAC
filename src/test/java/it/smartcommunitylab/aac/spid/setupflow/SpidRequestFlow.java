package it.smartcommunitylab.aac.spid.setupflow;

import it.smartcommunitylab.aac.spid.utils.RequestUtils;
import it.smartcommunitylab.aac.spid.utils.UserUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Orchestrates the SPID SAML 2.0 authentication flow for testing.
 * This builder simplifies test setup by automating the generation of AuthnRequests,
 * extracting essential SAML parameters, and dynamically building and signing mock IdP Responses.
 */
public class SpidRequestFlow {

    // Core test dependencies
    private final MockMvc mockMvc;

    /* --- Routing & Endpoints Configurations ---
     * Store the URLs needed to simulate the browser redirects between the Service Provider (AAC)
     * and the mock Identity Provider (IdP).
     */
    private String baseUrlAac;
    private String userDestinationUrl;
    private String authenticatePath;
    private String registrationId;

    // Internal utilities to keep the builder logic clean
    private final UserUtils userUtils = new UserUtils();
    private final RequestUtils requestUtils = new RequestUtils();

    // Flow execution flags and state
    private boolean generateSession = false;
    private boolean usePostBinding = false;

    /**
     * Internal constructor to initialize the Builder with the required dependencies.
     * @param mockMvc The Spring MockMvc context.
     */
    public SpidRequestFlow(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * Injects the basic routing URLs associated with the Service Provider environment.
     * @param baseUrlAac Base URL of the server.
     * @param userDestinationUrl Protected resource URL.
     * @param authenticatePath Path to initiate the SPID auth flow.
     * @return The current builder instance.
     */
    public SpidRequestFlow withEndpoints(String baseUrlAac, String userDestinationUrl, String authenticatePath) {
        this.baseUrlAac = baseUrlAac;
        this.userDestinationUrl = userDestinationUrl;
        this.authenticatePath = authenticatePath;
        return this;
    }

    /**
     * Injects the URLs and identifiers related to the mock Identity Provider.
     * @param registrationId The internal AAC registration ID for this provider.
     * @return The current builder instance.
     */
    public SpidRequestFlow withIdpConfig(String registrationId) {
        this.registrationId = registrationId;
        return this;
    }

    /** @return The current builder instance instructed to generate a valid Session. */
    public SpidRequestFlow withSession() {
        this.generateSession = true;
        return this;
    }

    /**
     * Configures the expected SAML 2.0 message binding for the AuthnRequest generation phase.
     * When set to true, the builder anticipates an HTTP-POST binding, extracting the SAML payload
     * from an auto-submitting HTML form (HTTP 200 OK). When false, it defaults to the standard
     * HTTP-Redirect binding, extracting the payload from the query parameters (HTTP 302 Found).
     *
     * @param usePostBinding true to enforce the HTTP-POST binding logic; false to use HTTP-Redirect.
     * @return The current builder instance.
     */
    public SpidRequestFlow withPostBinding(Boolean usePostBinding) {
        this.usePostBinding = usePostBinding;
        return this;
    }

    /**
     * Executes the requested flow and builds the final test context.
     * @return A consolidated SpidTestContext with all flow parameters.
     * @throws Exception If MockMvc execution, SAML decoding, XML manipulation, or signing fails.
     */
    public SpidRequest executeRequest() throws Exception {
        MockHttpSession session = null;
        String relayState = null;
        String requestId = null;
        String samlRequestEncoded = null;
        String xmlRequest = null;
        String redirectedUrl = null;

        // 1. Simulate the initialization of the SSO flow by the SP
        if (this.generateSession) {
            session = userUtils.createSessionWithSavedClientRequest(this.baseUrlAac);
            mockMvc.perform(get(this.userDestinationUrl).session(session));

            // Request on Login
            MvcResult result = mockMvc.perform(get(this.baseUrlAac + this.authenticatePath + this.registrationId)
                            .secure(true)
                            .session(session))
                    .andReturn();

            if (this.usePostBinding) {
                // --- HTTP-POST ---
                if ( result.getResponse().getStatus() != 200 ){
                    throw new IllegalArgumentException("Mismatch status for HTTP-POST");
                }

                String html = result.getResponse().getContentAsString();
                samlRequestEncoded = requestUtils.extractHtmlInputValue(html, "SAMLRequest");
                relayState = requestUtils.extractHtmlInputValue(html, "RelayState");

                // No Inflate
                xmlRequest = requestUtils.decodePostSamlRequest(samlRequestEncoded);
            } else {
                // --- HTTP-REDIRECT ---
                if ( result.getResponse().getStatus() != 302 ){
                    throw new IllegalArgumentException("Mismatch status for HTTP-REDIRECT");
                }

                redirectedUrl = result.getResponse().getRedirectedUrl();
                samlRequestEncoded = requestUtils.extractSamlRequestParameter(redirectedUrl);
                relayState = requestUtils.extractRelayStateParameter(redirectedUrl);

                // Inflate
                xmlRequest = requestUtils.decodeAndInflateSamlRequest(samlRequestEncoded);
            }

            requestId = requestUtils.extractAuthnRequestId(xmlRequest);
        }

        return new SpidRequest(session, relayState, samlRequestEncoded, xmlRequest, requestId, redirectedUrl);
    }
}
