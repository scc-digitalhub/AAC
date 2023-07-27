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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/*
 * Security policy for openAPI assets and console
 *
 * TODO evaluate config flag to enforce auth
 *
 */
@Configuration
@Order(14)
public class OpenAPISecurityConfig {

    /*
     * Configure a separated security context for openAPI
     */
    @Order(14)
    @Bean("openapiSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only endpoints
        http
            .requestMatcher(getRequestMatcher())
            // public access
            .authorizeRequests(requests -> requests.anyRequest().permitAll())
            // we don't want a session for these endpoints
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays
            .stream(OPENAPI_URLS)
            .map(u -> new AntPathRequestMatcher(u))
            .collect(Collectors.toList());

        return new OrRequestMatcher(antMatchers);
    }

    public static final String[] OPENAPI_URLS = {
        "/v3/api-docs",
        "/v3/api-docs.yaml",
        "/v3/api-docs/*",
        "/configuration/ui",
        "/swagger-resources/**",
        "/swagger-ui/**",
        "/configuration/**",
        "/swagger-ui.html",
        "/webjars/**",
    };
}
