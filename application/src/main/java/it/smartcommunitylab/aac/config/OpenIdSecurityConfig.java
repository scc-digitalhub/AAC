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
import it.smartcommunitylab.aac.oauth.auth.InternalOpaqueTokenIntrospector;
import it.smartcommunitylab.aac.openid.endpoint.UserInfoEndpoint;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/*
 * Security context for OpenId endpoints
 *
 * Builds a stateless context with JWT/OAuth2 auth.
 * We actually use Bearer tokens and validate by fetching tokens from store
 */

@Configuration
@Order(22)
public class OpenIdSecurityConfig {

    @Autowired
    private InternalOpaqueTokenIntrospector tokenIntrospector;

    /*
     * Configure a separated security context for API
     */
    @Order(22)
    @Bean("openidSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only endpoints
        http
            .requestMatcher(getRequestMatcher())
            .authorizeRequests(
                authorizeRequests -> authorizeRequests.anyRequest().hasAnyAuthority(Config.R_USER, Config.R_CLIENT)
            )
            // support passing token in request body via custom resolver
            .oauth2ResourceServer(
                oauth2 ->
                    oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .opaqueToken(opaqueToken -> opaqueToken.introspector(tokenIntrospector))
            )
            // disable request cache, we override redirects but still better enforce it
            .requestCache(requestCache -> requestCache.disable())
            .exceptionHandling()
            // use 401
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            .accessDeniedPage("/accesserror")
            .and()
            .cors()
            .configurationSource(corsConfigurationSource())
            .and()
            .csrf()
            .disable()
            // we don't want a session for these endpoints, each request should be evaluated
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        // support token in form body
        resolver.setAllowFormEncodedBodyParameter(true);
        return resolver;
    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays.stream(OPENID_URLS)
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

    public static final String[] OPENID_URLS = { UserInfoEndpoint.USERINFO_URL };
}
