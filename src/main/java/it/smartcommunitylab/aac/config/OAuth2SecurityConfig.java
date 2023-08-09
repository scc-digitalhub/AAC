/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import it.smartcommunitylab.aac.core.ClientAuthenticationManager;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthFilter;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientJwtAssertionAuthenticationProvider;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientPKCEAuthenticationProvider;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientRefreshAuthenticationProvider;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientSecretAuthenticationProvider;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenRevocationEndpoint;
import it.smartcommunitylab.aac.oauth.provider.PeekableAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CompositeFilter;

/*
 * Security context for oauth2 endpoints
 *
 * Builds a stateless context with oauth2 Client Auth
 */

@Configuration
@Order(21)
public class OAuth2SecurityConfig {

    @Value("${application.url}")
    private String applicationUrl;

    @Autowired
    private ClientDetailsService clientService;

    @Autowired
    private OAuth2ClientDetailsService oauth2ClientDetailsService;

    @Autowired
    private PeekableAuthorizationCodeServices authCodeServices;

    @Autowired
    private ExtTokenStore tokenStore;

    /*
     * Configure a separated security context for oauth2 tokenEndpoints
     */
    @Order(21)
    @Bean("oauth2SecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only client endpoints
        http
            .requestMatcher(getRequestMatcher())
            .authorizeRequests(authorizeRequests -> authorizeRequests.anyRequest().hasAnyAuthority(Config.R_CLIENT))
            // disable request cache, we override redirects but still better enforce it
            .requestCache(requestCache -> requestCache.disable())
            .exceptionHandling()
            // use custom entrypoint with error message
            .authenticationEntryPoint(new OAuth2AuthenticationEntryPoint())
            .accessDeniedPage("/accesserror")
            .and()
            .cors()
            .configurationSource(corsConfigurationSource())
            .and()
            .csrf()
            .disable()
            .addFilterBefore(
                getOAuth2ClientFilters(clientService, oauth2ClientDetailsService, authCodeServices, tokenStore),
                BasicAuthenticationFilter.class
            )
            // we don't want a session for these endpoints, each request should be evaluated
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

    private Filter getOAuth2ClientFilters(
        ClientDetailsService clientService,
        OAuth2ClientDetailsService clientDetailsService,
        PeekableAuthorizationCodeServices authCodeServices,
        ExtTokenStore tokenStore
    ) {
        // build auth providers for oauth2 clients
        OAuth2ClientPKCEAuthenticationProvider pkceAuthProvider = new OAuth2ClientPKCEAuthenticationProvider(
            clientDetailsService,
            authCodeServices
        );
        pkceAuthProvider.setClientService(clientService);
        OAuth2ClientSecretAuthenticationProvider secretAuthProvider = new OAuth2ClientSecretAuthenticationProvider(
            clientDetailsService
        );
        secretAuthProvider.setClientService(clientService);
        OAuth2ClientRefreshAuthenticationProvider refreshAuthProvider = new OAuth2ClientRefreshAuthenticationProvider(
            clientDetailsService,
            tokenStore
        );
        refreshAuthProvider.setClientService(clientService);

        // build audience for all endpoints
        Set<String> audience = new HashSet<>();
        audience.add(applicationUrl);
        audience.add(applicationUrl + TOKEN_ENDPOINT);
        audience.add(applicationUrl + TOKEN_INTROSPECT_ENDPOINT);
        audience.add(applicationUrl + TOKEN_REVOKE_ENDPOINT);
        // build jwt client auth provider for given endpoints
        OAuth2ClientJwtAssertionAuthenticationProvider jwtAssertionProvider =
            new OAuth2ClientJwtAssertionAuthenticationProvider(clientDetailsService, audience);
        jwtAssertionProvider.setClientService(clientService);

        ClientAuthenticationManager authManager = new ClientAuthenticationManager(
            secretAuthProvider,
            jwtAssertionProvider
        );
        authManager.setClientService(clientService);

        ClientAuthenticationManager pkceAuthManager = new ClientAuthenticationManager(
            secretAuthProvider,
            pkceAuthProvider,
            refreshAuthProvider,
            jwtAssertionProvider
        );
        pkceAuthManager.setClientService(clientService);

        // TODO add realm style endpoints
        OAuth2ClientAuthFilter tokenEndpointFilter = new OAuth2ClientAuthFilter(pkceAuthManager, TOKEN_ENDPOINT);
        OAuth2ClientAuthFilter tokenIntrospectFilter = new OAuth2ClientAuthFilter(
            authManager,
            TOKEN_INTROSPECT_ENDPOINT
        );
        OAuth2ClientAuthFilter tokenRevokeFilter = new OAuth2ClientAuthFilter(authManager, TOKEN_REVOKE_ENDPOINT);

        List<Filter> filters = new ArrayList<>();
        filters.add(tokenEndpointFilter);
        filters.add(tokenIntrospectFilter);
        filters.add(tokenRevokeFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays
            .stream(OAUTH2_CLIENT_URLS)
            .map(u -> new AntPathRequestMatcher(u))
            .collect(Collectors.toList());

        return new OrRequestMatcher(antMatchers);
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST"));
        config.setAllowedHeaders(Arrays.asList("authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static final String TOKEN_ENDPOINT = TokenEndpoint.TOKEN_URL;
    private static final String TOKEN_INTROSPECT_ENDPOINT = TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL;
    private static final String TOKEN_REVOKE_ENDPOINT = TokenRevocationEndpoint.TOKEN_REVOCATION_URL;

    private static final String[] OAUTH2_CLIENT_URLS = {
        TOKEN_ENDPOINT,
        TOKEN_INTROSPECT_ENDPOINT,
        TOKEN_REVOKE_ENDPOINT,
    };
}
