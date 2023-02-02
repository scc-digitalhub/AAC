package it.smartcommunitylab.aac.saml.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.util.Assert;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;

public class HttpSessionSaml2AuthenticationRequestRepository
        implements Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> {

    private static final String DEFAULT_AUTHENTICATION_REQUEST_ATTR_NAME = HttpSessionSaml2AuthenticationRequestRepository.class
            .getName() + ".SAML2_SER_AUTHORIZATION_REQUEST";

    private final String sessionAttributeName;

    public HttpSessionSaml2AuthenticationRequestRepository() {
        this(DEFAULT_AUTHENTICATION_REQUEST_ATTR_NAME);
    }

    public HttpSessionSaml2AuthenticationRequestRepository(String sessionAttributeName) {
        Assert.hasText(sessionAttributeName, "session attribute name can not be null or empty");
        this.sessionAttributeName = sessionAttributeName;
    }

    @Override
    public SerializableSaml2AuthenticationRequestContext loadAuthenticationRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");

        String relayState = this.getRelayStateParameter(request);
        if (relayState == null) {
            return null;
        }

        Map<String, SerializableSaml2AuthenticationRequestContext> authenticationRequests = this
                .getAuthenticationRequests(request);
        return authenticationRequests.get(relayState);
    }

    @Override
    public void saveAuthenticationRequest(SerializableSaml2AuthenticationRequestContext authenticationRequest,
            HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");

        if (authenticationRequest == null) {
            this.removeAuthenticationRequest(request, response);
            return;
        }

        String relayState = authenticationRequest.getRelayState();
        Assert.hasText(relayState, "relayState cannot be empty");

        Map<String, SerializableSaml2AuthenticationRequestContext> authenticationRequests = this
                .getAuthenticationRequests(request);
        authenticationRequests.put(relayState, authenticationRequest);
        request.getSession().setAttribute(this.sessionAttributeName, authenticationRequests);

    }

    @Override
    public SerializableSaml2AuthenticationRequestContext removeAuthenticationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");

        String relayState = this.getRelayStateParameter(request);
        if (relayState == null) {
            return null;
        }

        Map<String, SerializableSaml2AuthenticationRequestContext> authenticationRequests = this
                .getAuthenticationRequests(request);
        SerializableSaml2AuthenticationRequestContext originalRequest = authenticationRequests.remove(relayState);
        if (!authenticationRequests.isEmpty()) {
            request.getSession().setAttribute(this.sessionAttributeName, authenticationRequests);
        } else {
            request.getSession().removeAttribute(this.sessionAttributeName);
        }
        return originalRequest;
    }

    private String getRelayStateParameter(HttpServletRequest request) {
        // handle case insensitive lookup
        String relayState = null;
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if ("relaystate".equals(entry.getKey().toLowerCase())) {
                relayState = entry.getValue()[0];
                break;
            }
        }

        return relayState;
    }

    @SuppressWarnings("unchecked")
    private Map<String, SerializableSaml2AuthenticationRequestContext> getAuthenticationRequests(
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        Map<String, SerializableSaml2AuthenticationRequestContext> authenticationRequests = (session != null)
                ? (Map<String, SerializableSaml2AuthenticationRequestContext>) session
                        .getAttribute(this.sessionAttributeName)
                : null;

        if (authenticationRequests == null) {
            return new HashMap<>();
        }

        return authenticationRequests;
    }

}
