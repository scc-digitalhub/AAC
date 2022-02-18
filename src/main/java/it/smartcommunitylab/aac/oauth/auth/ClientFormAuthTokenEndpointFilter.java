package it.smartcommunitylab.aac.oauth.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import it.smartcommunitylab.aac.core.auth.NoOpAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;

/**
 * Filter for the client credential token acquisition. Extends the standard
 * behavior in case of authorization code flow to support PKCE
 * 
 * 
 * @author raman
 *
 */
public class ClientFormAuthTokenEndpointFilter extends AbstractAuthenticationProcessingFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_FILTER_URI = "/oauth/token";

    private AuthenticationSuccessHandler successHandler = new NoOpAuthenticationSuccessHandler();
    private AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();

    public ClientFormAuthTokenEndpointFilter() {
        this(DEFAULT_FILTER_URI);
    }

    public ClientFormAuthTokenEndpointFilter(String filterProcessingUrl) {
        super(DEFAULT_FILTER_URI);
        // override request matcher
        RequestMatcher requestMatcher = new ClientCredentialsRequestMatcher(
                new AntPathRequestMatcher(filterProcessingUrl));
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // configure custom filter behavior
        setSessionStrategy();
        setHandlers();
    }

    public ClientFormAuthTokenEndpointFilter(String... filterProcessingUrl) {
        super(DEFAULT_FILTER_URI);
        // override request matcher, we want to support global AND realm paths
        List<RequestMatcher> antMatchers = Arrays.stream(filterProcessingUrl).map(u -> new AntPathRequestMatcher(u))
                .collect(Collectors.toList());
        RequestMatcher requestMatcher = new ClientCredentialsRequestMatcher(new OrRequestMatcher(antMatchers));
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // configure custom filter behavior
        setSessionStrategy();
        setHandlers();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException, IOException, ServletException {

        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[] { "POST" });
        }

        String clientId = request.getParameter("client_id");
        String clientSecret = request.getParameter("client_secret");

        // If the request is already authenticated we can assume that this
        // filter is not needed
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication;
        }

        if (!StringUtils.hasText(clientId)) {
            throw new BadCredentialsException("No client credentials presented");
        }

        if (clientSecret == null) {
            clientSecret = "";
        }

        clientId = clientId.trim();
        AbstractAuthenticationToken authRequest = new OAuth2ClientSecretAuthenticationToken(clientId,
                clientSecret, AuthenticationMethod.CLIENT_SECRET_POST.getValue());

        // PKCE requests can avoid secret, let's check
        if (!StringUtils.hasText(clientSecret)) {
            String grantType = request.getParameter("grant_type");
            if ("authorization_code".equals(grantType)
                    && request.getParameterMap().containsKey("code_verifier")) {
                String code = request.getParameter("code");
                String verifier = request.getParameter("code_verifier");
                // replace request
                authRequest = new OAuth2ClientPKCEAuthenticationToken(clientId, code, verifier,
                        AuthenticationMethod.NONE.getValue());
            }

            // support refresh flow with pkce without secret
            // requires refresh token rotation set
            if ("refresh_token".equals(grantType)
                    && request.getParameterMap().containsKey("refresh_token")) {
                String refreshToken = request.getParameter("refresh_token");
                // replace request
                authRequest = new OAuth2ClientRefreshAuthenticationToken(clientId, refreshToken,
                        AuthenticationMethod.NONE.getValue());
            }
        }

        // collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        authRequest.setDetails(webAuthenticationDetails);

        // let authManager process request
        return this.getAuthenticationManager().authenticate(authRequest);

    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        chain.doFilter(request, response);
    }

    private void setSessionStrategy() {
        // enforce session id change, calls to tokenEndpoint should not leverage
        // existing sessions. We will invalidate session after response.
        setAllowSessionCreation(true);
        setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
    }

    private void setHandlers() {
        // override success handler to avoid redirect strategies, we just need auth in
        // context
        setAuthenticationSuccessHandler(successHandler);
        // also set a custom failure handler to translate to oauth2 errors
        setAuthenticationFailureHandler(new AuthenticationFailureHandler() {
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException exception) throws IOException, ServletException {
                if (exception instanceof BadCredentialsException) {
                    exception = new BadCredentialsException(exception.getMessage(),
                            new BadClientCredentialsException());
                }
                authenticationEntryPoint.commence(request, response, exception);
            }
        });
    }

    protected static class ClientCredentialsRequestMatcher implements RequestMatcher {
        private RequestMatcher requestMatcher;

        public ClientCredentialsRequestMatcher(RequestMatcher requestMatcher) {
            Assert.notNull(requestMatcher, "request matcher is mandatory");
            this.requestMatcher = requestMatcher;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            if (requestMatcher.matches(request)) {

                String clientId = request.getParameter("client_id");

                if (clientId == null) {
                    // Give basic auth a chance to work instead (it's preferred anyway)
                    return false;
                }

                // check if basic auth + clientId is provided
                String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.startsWithIgnoreCase(header, "Basic")) {
                    // Give basic auth a chance to work instead (it's preferred anyway)
                    return false;
                }

                return true;
            } else {
                return false;
            }
        }
    }
}
