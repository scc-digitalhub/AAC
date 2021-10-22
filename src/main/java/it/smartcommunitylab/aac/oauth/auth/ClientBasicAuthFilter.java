package it.smartcommunitylab.aac.oauth.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.auth.NoOpAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;

public class ClientBasicAuthFilter extends AbstractAuthenticationProcessingFilter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();
    private BasicAuthenticationConverter authenticationConverter = new BasicAuthenticationConverter();
    private AuthenticationSuccessHandler successHandler = new NoOpAuthenticationSuccessHandler();

    public ClientBasicAuthFilter(String filterProcessingUrl) {
        super(filterProcessingUrl);
        // override request matcher
        RequestMatcher requestMatcher = new BasicAuthRequestMatcher(new AntPathRequestMatcher(filterProcessingUrl));
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // configure custom filter behavior
        setSessionStrategy();
        setHandlers();
    }

    public ClientBasicAuthFilter(String... filterProcessingUrl) {
        super(filterProcessingUrl[0]);
        // override request matcher, we want to support global AND realm paths
        List<RequestMatcher> antMatchers = Arrays.stream(filterProcessingUrl).map(u -> new AntPathRequestMatcher(u))
                .collect(Collectors.toList());
        RequestMatcher requestMatcher = new BasicAuthRequestMatcher(new OrRequestMatcher(antMatchers));
        setRequiresAuthenticationRequestMatcher(requestMatcher);

        // configure custom filter behavior
        setSessionStrategy();
        setHandlers();
    }

    /**
     * @param authenticationEntryPoint the authentication entry point to set
     */
    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
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

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
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
        setAuthenticationSuccessHandler(new AuthenticationSuccessHandler() {
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                // no-op - just allow filter chain to continue to token endpoint
            }
        });
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        // use basic auth converter, will throw exception if header is malformed
        UsernamePasswordAuthenticationToken basicRequest = this.authenticationConverter.convert(request);

        // TODO support PKCE with basic auth with only clientId
        // out of spec but possible
        // NOTE: as per https://www.rfc-editor.org/rfc/rfc6749#section-2.3.1
        // both clientId and secret are form encoded
        String clientId = decodeClientCredential((String) basicRequest.getPrincipal());
        String clientSecret = decodeClientCredential((String) basicRequest.getCredentials());

        if (!StringUtils.hasText(clientId)) {
            throw new BadCredentialsException("No client credentials presented");
        }

        clientId = clientId.trim();

        if (clientSecret == null) {
            clientSecret = "";
        }

        // basic auth fields are urlencoded
        // as per https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1
        clientId = URLDecoder.decode(clientId, "UTF-8");
        clientSecret = URLDecoder.decode(clientSecret, "UTF-8");

        // convert to our authRequest
        AbstractAuthenticationToken authRequest = new OAuth2ClientSecretAuthenticationToken(clientId,
                clientSecret, AuthenticationMethod.CLIENT_SECRET_BASIC.getValue());

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

    protected static class BasicAuthRequestMatcher implements RequestMatcher {
        private RequestMatcher requestMatcher;

        public BasicAuthRequestMatcher(RequestMatcher requestMatcher) {
            Assert.notNull(requestMatcher, "request matcher is mandatory");
            this.requestMatcher = requestMatcher;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            if (requestMatcher.matches(request)) {

                // check if basic auth header
                String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                return StringUtils.startsWithIgnoreCase(header, "Basic");

            } else {
                return false;
            }
        }
    }

    private String decodeClientCredential(String credential) {
        try {
            return URLDecoder.decode(credential, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // not going to happen but in case we'll pass through
            return credential;
        }
    }

}
