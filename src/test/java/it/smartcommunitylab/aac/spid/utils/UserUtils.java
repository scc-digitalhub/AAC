package it.smartcommunitylab.aac.spid.utils;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

/**
 * Utility class for mocking user-related entities and session states
 * during SPID authentication tests.
 * Provides mock subjects, authenticated sessions, and pre-configured
 * Spring Security contexts to simulate successful or pending login flows.
 */
public class UserUtils {

    /**
     * Simulates an HTTP session where a user has attempted to access a protected resource
     * (e.g., the '/console/user' endpoint) before being authenticated.
     * Spring Security's HttpSessionRequestCache saves this original request so the user
     * can be redirected back to it after a successful SPID login.
     *
     * @return A pre-populated {@link MockHttpSession} containing the saved target request.
     */
    public MockHttpSession createSessionWithSavedClientRequest(String ssoDestinationUrl) {
        MockHttpSession session = new MockHttpSession();

        String[] urlParts = ssoDestinationUrl.split("://");
        String scheme = urlParts[0];
        String hostAndPort = urlParts[1];

        MockHttpServletRequest savedRequest = new MockHttpServletRequest("GET", "/console/user");
        savedRequest.setScheme(scheme);
        savedRequest.setServerName(hostAndPort);
        savedRequest.setSession(session);

        // Save the request in the session, simulating Spring Security's ExceptionTranslationFilter behavior
        new HttpSessionRequestCache().saveRequest(savedRequest, new MockHttpServletResponse());
        return (MockHttpSession) savedRequest.getSession();
    }
}
