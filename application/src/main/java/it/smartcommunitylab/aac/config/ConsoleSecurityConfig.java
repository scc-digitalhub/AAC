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

import it.smartcommunitylab.aac.core.auth.Http401UnauthorizedEntryPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/*
 * Security context for console endpoints
 *
 * Builds a stateful context with no auth entrypoint
 */

@Configuration
@Order(25)
public class ConsoleSecurityConfig {

    @Value("${security.console.cors.origins}")
    private String corsOrigins;

    /*
     * Configure a separated security context for API
     */
    @Order(25)
    @Bean("consoleSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only console endpoints and require user access
        http
            .requestMatcher(getRequestMatcher())
            .authorizeRequests(authorizeRequests -> authorizeRequests.anyRequest().hasAnyAuthority("ROLE_USER"))
            // disable request cache, we override redirects but still better enforce it
            .requestCache(requestCache -> requestCache.disable())
            .exceptionHandling()
            // use 401
            .authenticationEntryPoint(new Http401UnauthorizedEntryPoint())
            //                .accessDeniedPage("/accesserror")
            .and()
            // disable crsf for spa console
            .csrf()
            .disable()
            // we want a session for console
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);

        if (StringUtils.hasText(corsOrigins)) {
            // allow cors
            http.cors().configurationSource(corsConfigurationSource(corsOrigins));
        }

        return http.build();
    }

    @Order(25)
    @Bean("h2ConsoleSecurityFilterChain")
    @ConditionalOnProperty(prefix = "spring", name = "h2.console.enabled")
    SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .requestMatcher(PathRequest.toH2Console())
            .authorizeHttpRequests(authorizeRequests -> authorizeRequests.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    public RequestMatcher getRequestMatcher() {
        // skip index.html to let auth entry point enforce login
        List<RequestMatcher> matchers = Arrays.stream(CONSOLES)
            .map(c -> new NegatedRequestMatcher(new AntPathRequestMatcher(CONSOLE_PREFIX + "/" + c)))
            .collect(Collectors.toList());

        List<RequestMatcher> antMatchers = new ArrayList<>(matchers);
        antMatchers.add(new AntPathRequestMatcher(CONSOLE_PREFIX + "/**"));

        return new AndRequestMatcher(antMatchers);
    }

    private CorsConfigurationSource corsConfigurationSource(String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(new ArrayList<>(StringUtils.commaDelimitedListToSet(origins)));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setExposedHeaders(Arrays.asList("authorization", "range"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    public static final String CONSOLE_PREFIX = "/console";

    public static String[] CONSOLES = { "user", "dev", "admin" };
}
