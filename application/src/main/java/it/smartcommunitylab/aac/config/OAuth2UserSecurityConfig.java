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
import it.smartcommunitylab.aac.core.auth.ExtendedLoginUrlAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.LoginUrlRequestConverter;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.identity.service.IdentityProviderService;
import it.smartcommunitylab.aac.oauth.auth.AuthorizationEndpointFilter;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAwareLoginUrlConverter;
import it.smartcommunitylab.aac.oauth.auth.OAuth2IdpAwareLoginUrlConverter;
import it.smartcommunitylab.aac.oauth.endpoint.AuthorizationEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.UserApprovalEndpoint;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;
import it.smartcommunitylab.aac.oauth.store.AuthorizationRequestStore;
import it.smartcommunitylab.aac.openid.endpoint.EndSessionEndpoint;
import it.smartcommunitylab.aac.password.auth.InternalPasswordResetOnAccessFilter;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordEntityRepository;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.tos.TosOnAccessFilter;
import it.smartcommunitylab.aac.users.service.UserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
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
@Order(28)
public class OAuth2UserSecurityConfig {

    @Value("${application.url}")
    private String applicationUrl;

    private final String loginPath = "/login";

    @Autowired
    private OAuth2ClientService clientService;

    @Autowired
    private OAuth2ClientDetailsService clientDetailsService;

    @Autowired
    private IdentityProviderAuthorityService authorityService;

    @Autowired
    private IdentityProviderService idpProviderService;

    @Autowired
    private InternalUserPasswordEntityRepository passwordRepository;

    @Autowired
    private ProviderConfigRepository<PasswordIdentityProviderConfig> internalPasswordIdentityProviderConfigRepository;

    @Autowired
    private RealmService realmService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthorizationRequestStore authorizationRequestStore;

    /*
     * Configure a separated security context for oauth2 tokenEndpoints
     */
    @Order(28)
    @Bean("oauth2UserSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only user endpoints
        http
            .requestMatcher(getRequestMatcher())
            //.authorizeRequests().anyRequest().authenticated().and()
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .antMatchers(END_SESSION_ENDPOINT)
                    .permitAll()
                    .anyRequest()
                    .hasAnyAuthority(Config.R_USER)
            )
            .exceptionHandling()
            .authenticationEntryPoint(
                authEntryPoint(
                    loginPath,
                    authorityService,
                    idpProviderService,
                    clientDetailsService,
                    authorizationRequestStore
                )
            )
            .accessDeniedPage("/accesserror")
            .and()
            .cors()
            .configurationSource(corsConfigurationSource())
            .and()
            .csrf()
            .ignoringAntMatchers(AUTHORIZATION_ENDPOINT, END_SESSION_ENDPOINT)
            .and()
            .addFilterBefore(
                getOAuth2UserFilters(
                    idpProviderService,
                    clientDetailsService,
                    clientService,
                    authorizationRequestStore,
                    loginPath
                ),
                BasicAuthenticationFilter.class
            )
            .addFilterAfter(getSessionFilters(), BasicAuthenticationFilter.class)
            // we do want a valid user session for these endpoints
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);

        return http.build();
    }

    //    @Bean
    //    public SecurityContextHolderAwareRequestFilter securityContextHolderAwareRequestFilter() {
    //        return new SecurityContextHolderAwareRequestFilter();
    //    }

    public Filter getOAuth2UserFilters(
        IdentityProviderService providerService,
        OAuth2ClientDetailsService oauth2ClientDetailsService,
        OAuth2ClientService oauth2ClientService,
        AuthorizationRequestStore authorizationRequestStore,
        String loginUrl
    ) {
        AuthorizationEndpointFilter authorizationFilter = new AuthorizationEndpointFilter(
            oauth2ClientService,
            oauth2ClientDetailsService,
            authorizationRequestStore
        );
        LoginUrlRequestConverter clientAwareConverter = new OAuth2ClientAwareLoginUrlConverter(
            oauth2ClientDetailsService,
            authorizationRequestStore,
            loginUrl
        );
        ExtendedLoginUrlAuthenticationEntryPoint entryPoint = new ExtendedLoginUrlAuthenticationEntryPoint(
            loginUrl,
            clientAwareConverter
        );
        authorizationFilter.setAuthenticationEntryPoint(entryPoint);
        return authorizationFilter;
    }

    private Filter getSessionFilters() {
        InternalPasswordResetOnAccessFilter passwordResetFilter = new InternalPasswordResetOnAccessFilter(
            passwordRepository,
            internalPasswordIdentityProviderConfigRepository
        );

        TosOnAccessFilter tosFilter = new TosOnAccessFilter(realmService, userService);

        // disable logout post-reset for oauth2 urls
        passwordResetFilter.setLogoutAfterReset(false);

        // build a virtual filter chain as composite filter
        ArrayList<Filter> filters = new ArrayList<>();
        filters.add(passwordResetFilter);
        filters.add(tosFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    private AuthenticationEntryPoint authEntryPoint(
        String loginUrl,
        IdentityProviderAuthorityService authorityService,
        IdentityProviderService providerService,
        OAuth2ClientDetailsService oauth2ClientDetailsService,
        AuthorizationRequestStore authorizationRequestStore
    ) {
        ExtendedLoginUrlAuthenticationEntryPoint entryPoint = new ExtendedLoginUrlAuthenticationEntryPoint(loginUrl);
        List<LoginUrlRequestConverter> converters = new ArrayList<>();
        LoginUrlRequestConverter idpAwareConverter = new OAuth2IdpAwareLoginUrlConverter(
            idpProviderService,
            authorityService
        );
        LoginUrlRequestConverter clientAwareConverter = new OAuth2ClientAwareLoginUrlConverter(
            oauth2ClientDetailsService,
            authorizationRequestStore,
            loginUrl
        );

        converters.add(idpAwareConverter);
        converters.add(clientAwareConverter);
        entryPoint.setConverters(converters);

        return entryPoint;
    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays
            .stream(OAUTH2_USER_URLS)
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

    private static final String AUTHORIZATION_ENDPOINT = AuthorizationEndpoint.AUTHORIZATION_URL;
    private static final String USER_APPROVAL_ENDPOINT = UserApprovalEndpoint.APPROVAL_URL;
    private static final String ACCESS_CONFIRMATION_ENDPOINT = UserApprovalEndpoint.ACCESS_CONFIRMATION_URL;
    private static final String END_SESSION_ENDPOINT = EndSessionEndpoint.END_SESSION_URL;

    private static final String[] OAUTH2_USER_URLS = {
        AUTHORIZATION_ENDPOINT,
        USER_APPROVAL_ENDPOINT,
        ACCESS_CONFIRMATION_ENDPOINT,
        END_SESSION_ENDPOINT,
    };
}
