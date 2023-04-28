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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import it.smartcommunitylab.aac.core.auth.DelegatingAuthenticationConverter;

public class OAuth2ClientAuthFilter extends OncePerRequestFilter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();
    private SessionAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();

    private final RequestMatcher requestMatcher;
    private final AuthenticationManager authenticationManager;
    private final AuthenticationConverter authenticationConverter;

    private final String name;

    public OAuth2ClientAuthFilter(AuthenticationManager authenticationManager,
            String filterProcessingUrl) {
        Assert.notNull(authenticationManager, "auth manager is required");
        Assert.hasText(filterProcessingUrl, "filterProcessingUrl can not be null or empty");
        this.authenticationManager = authenticationManager;

        // build auth converters ordered by priority
        this.authenticationConverter = new DelegatingAuthenticationConverter(
                new ClientJwtAssertionAuthenticationConverter(),
                new ClientPKCEAuthenticationConverter(),
                new ClientRefreshAuthenticationConverter(),
                new ClientSecretPostAuthenticationConverter(),
                new ClientSecretBasicAuthenticationConverter());

        // build request matcher
        requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);

        // set unique name based on url
        name = filterProcessingUrl;
    }

    public OAuth2ClientAuthFilter(AuthenticationManager authenticationManager,
            AuthenticationConverter authenticationConverter,
            String filterProcessingUrl) {
        Assert.notNull(authenticationManager, "auth manager is required");
        Assert.notNull(authenticationConverter, "auth converter is required");
        Assert.hasText(filterProcessingUrl, "filterProcessingUrl can not be null or empty");
        this.authenticationManager = authenticationManager;
        this.authenticationConverter = authenticationConverter;

        // build request matcher
        requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);

        // set unique name based on url
        name = filterProcessingUrl;
    }

    public OAuth2ClientAuthFilter(AuthenticationManager authenticationManager,
            AuthenticationConverter authenticationConverter,
            String... filterProcessingUrl) {
        Assert.notNull(authenticationManager, "auth manager is required");
        Assert.notNull(authenticationConverter, "auth converter is required");
        Assert.notEmpty(filterProcessingUrl, "filterProcessingUrl can not be null or empty");
        this.authenticationManager = authenticationManager;
        this.authenticationConverter = authenticationConverter;

        // configure OR request matcher, we want to support global AND realm paths
        List<RequestMatcher> antMatchers = Arrays.stream(filterProcessingUrl)
                .filter(u -> StringUtils.hasText(u))
                .map(u -> new AntPathRequestMatcher(u))
                .collect(Collectors.toList());
        Assert.notEmpty(antMatchers, "filterProcessingUrl can not be null or empty");

        requestMatcher = new OrRequestMatcher(antMatchers);

        // set unique name based on url
        name = StringUtils.arrayToDelimitedString(filterProcessingUrl, "|");
    }

    @Override
    protected String getFilterName() {
        return getClass().getName() + this.name;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!this.requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication authResult = attemptAuthentication(request, response);
            if (authResult == null) {
                // nothing to do
                filterChain.doFilter(request, response);
                return;
            }

            // check token
            if (!(authResult instanceof OAuth2ClientAuthenticationToken)) {
                // something bad happened
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
            }

            // check client details
            OAuth2ClientAuthenticationToken token = (OAuth2ClientAuthenticationToken) authResult;
            if (token.getOAuth2ClientDetails() == null || token.getClient() == null) {
                // something bad happened
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
            }

            // make sure session id is changed
            this.sessionStrategy.onAuthentication(authResult, request, response);

            // replace security context from scratch
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authResult);
            SecurityContextHolder.setContext(context);

            // TODO evaluate event audit

            // continue processing filter chain with authentication in place
            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            // delegate response
            authenticationEntryPoint.commence(request, response, ex);
        }

    }

    public Authentication attemptAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException, IOException, ServletException {

        // try conversion
        Authentication authRequest = this.authenticationConverter.convert(request);
        if (authRequest == null) {
            return null;
        }

        // make sure this is a client auth
        // TODO rework
        if (!(authRequest instanceof OAuth2ClientAuthenticationToken)) {
            return null;
        }

        // let authManager process request
        return this.authenticationManager.authenticate(authRequest);
    }

}
