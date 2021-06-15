package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.CompositeFilter;

import it.smartcommunitylab.aac.core.ClientAuthenticationManager;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.oauth.PeekableAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.auth.ClientBasicAuthFilter;
import it.smartcommunitylab.aac.oauth.auth.ClientFormAuthTokenEndpointFilter;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientPKCEAuthenticationProvider;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientSecretAuthenticationProvider;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

/*
 * Security context for oauth2 endpoints
 * 
 * Builds a stateless context with oauth2 Client Auth
 */

@Configuration
@Order(11)
public class OAuth2SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ClientDetailsService clientService;

    @Autowired
    private OAuth2ClientDetailsService clientDetailsService;

    @Autowired
    private PeekableAuthorizationCodeServices authCodeServices;

    /*
     * Configure a separated security context for oauth2 tokenEndpoints
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // match only token endpoints
        http.requestMatcher(getRequestMatcher())
                .authorizeRequests((authorizeRequests) -> authorizeRequests
                        .anyRequest().hasAnyAuthority("ROLE_CLIENT"))
                // disable request cache, we override redirects but still better enforce it
                .requestCache((requestCache) -> requestCache.disable())
                .exceptionHandling()
                // use 401
//                .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedPage("/accesserror")
                .and()
                .csrf()
                .disable()
                .addFilterBefore(
                        getOAuth2ProviderFilters(clientService, clientDetailsService, authCodeServices),
                        BasicAuthenticationFilter.class);

        // we don't want a session for these endpoints, each request should be evaluated
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    private Filter getOAuth2ProviderFilters(
            ClientDetailsService clientService,
            OAuth2ClientDetailsService clientDetailsService,
            PeekableAuthorizationCodeServices authCodeServices) {

        // build auth providers for oauth2 clients
        OAuth2ClientPKCEAuthenticationProvider pkceAuthProvider = new OAuth2ClientPKCEAuthenticationProvider(
                clientDetailsService, authCodeServices);
        pkceAuthProvider.setClientService(clientService);
        OAuth2ClientSecretAuthenticationProvider secretAuthProvider = new OAuth2ClientSecretAuthenticationProvider(
                clientDetailsService);
        secretAuthProvider.setClientService(clientService);

        ClientAuthenticationManager authManager = new ClientAuthenticationManager(secretAuthProvider);
        authManager.setClientService(clientService);

        ClientAuthenticationManager pkceAuthManager = new ClientAuthenticationManager(secretAuthProvider,
                pkceAuthProvider);
        pkceAuthManager.setClientService(clientService);

        // build auth filters for TokenEndpoint
        // TODO add realm style endpoints
        ClientFormAuthTokenEndpointFilter formTokenEndpointFilter = new ClientFormAuthTokenEndpointFilter();
        formTokenEndpointFilter.setAuthenticationManager(pkceAuthManager);

        // TODO consolidate basicFilter for all endpoints
        ClientBasicAuthFilter basicTokenEndpointFilter = new ClientBasicAuthFilter("/oauth/token");
        basicTokenEndpointFilter.setAuthenticationManager(pkceAuthManager);

        ClientBasicAuthFilter basicTokenIntrospectFilter = new ClientBasicAuthFilter("/oauth/introspect");
        basicTokenIntrospectFilter.setAuthenticationManager(authManager);

        ClientBasicAuthFilter basicTokenRevokeFilter = new ClientBasicAuthFilter("/oauth/revoke");
        basicTokenRevokeFilter.setAuthenticationManager(authManager);

        List<Filter> filters = new ArrayList<>();
        filters.add(basicTokenEndpointFilter);
        filters.add(formTokenEndpointFilter);
        filters.add(basicTokenIntrospectFilter);
        filters.add(basicTokenRevokeFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays.stream(OAUTH2_URLS).map(u -> new AntPathRequestMatcher(u))
                .collect(Collectors.toList());

        return new OrRequestMatcher(antMatchers);

    }

    public static final String[] OAUTH2_URLS = {
            "/oauth/token",
            "/oauth/introspect",
            "/oauth/revoke"
    };

}