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

import it.smartcommunitylab.aac.core.ExtendedUserAuthenticationManager;
import it.smartcommunitylab.aac.core.authorities.AuthorityService;
import it.smartcommunitylab.aac.core.authorities.ConfigurableAuthorityService;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.identity.IdentityProviderAuthority;
import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.CompositeFilter;

@Configuration
@Order(17)
public class AuthConfig {

    @Autowired
    //    private IdentityProviderAuthorityService identityProviderAuthorityService;
    private AuthorityService<IdentityProviderAuthority<?, ?, ?>> identityProviderAuthorityService;

    @Autowired
    private ExtendedUserAuthenticationManager authManager;

    @Value("${application.url}")
    private String applicationURL;

    @Bean
    @Order(17)
    @Qualifier("authSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(getRequestMatcher())
            .authorizeRequests()
            .anyRequest()
            .permitAll()
            .and()
            .authenticationManager(authManager)
            .csrf()
            .ignoringRequestMatchers(buildAuthoritiesCorsMatcher())
            .and()
            .addFilterBefore(buildAuthoritiesFilters(), BasicAuthenticationFilter.class)
            // we always want a session here
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.ALWAYS);

        return http.build();
    }

    private Filter buildAuthoritiesFilters() {
        List<Filter> filters = new ArrayList<>();

        // build filters for every authority
        Collection<IdentityProviderAuthority<?, ?, ?>> authorities = identityProviderAuthorityService.getAuthorities();

        for (IdentityProviderAuthority<?, ?, ?> authority : authorities) {
            // build filters for this authority via filterProvider from authority itself
            FilterProvider provider = authority.getFilterProvider();
            if (provider != null) {
                // we expect a list of filters
                Collection<Filter> pfs = provider.getAuthFilters();
                if (filters != null) {
                    for (Filter filter : pfs) {
                        // register authManager for authFilters
                        if (filter instanceof AbstractAuthenticationProcessingFilter) {
                            ((AbstractAuthenticationProcessingFilter) filter).setAuthenticationManager(authManager);
                        }

                        filters.add(filter);
                    }
                }
            }
        }

        // build a virtual filter chain as composite filter
        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    private RequestMatcher buildAuthoritiesCorsMatcher() {
        List<RequestMatcher> antMatchers = new ArrayList<>();

        identityProviderAuthorityService
            .getAuthorities()
            .forEach(authority -> {
                FilterProvider provider = authority.getFilterProvider();
                if (provider != null) {
                    Collection<String> patterns = provider.getCorsIgnoringAntMatchers();
                    if (patterns != null) {
                        for (String pattern : patterns) {
                            antMatchers.add(new AntPathRequestMatcher(pattern));
                        }
                    }
                }
            });

        return new OrRequestMatcher(antMatchers);
    }

    private RequestMatcher getRequestMatcher() {
        return new AntPathRequestMatcher(AUTH_URL);
    }

    private static final String AUTH_URL = "/auth/**";
}
