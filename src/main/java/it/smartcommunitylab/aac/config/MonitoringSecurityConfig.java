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

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/*
 * Security context for actuator endpoints
 *
 * Disables authentication, could be extented to add basic auth
 */

@Configuration
@Order(26)
public class MonitoringSecurityConfig {

    @Value("${management.server.port}")
    private int managementPort;

    @Value("${management.endpoints.web.exposure.include}")
    private String[] endpoints;

    @Value("${management.endpoints.web.base-path}")
    private String basePath;

    /*
     * Configure a separated security context for management
     */
    @Order(26)
    @Bean("monitoringSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only actuator endpoints
        http
            .securityMatcher(getRequestMatcher())
            .authorizeRequests(requests -> requests.anyRequest().permitAll())
            .exceptionHandling()
            // use 403
            .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
            // we don't want a session for these endpoints
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> endpointMatchers = Arrays
            .stream(endpoints)
            .map(u -> new AntPathRequestMatcher(basePath + "/" + u + "/**"))
            .collect(Collectors.toList());
        List<RequestMatcher> antMatchers = new ArrayList<>(endpointMatchers);
        antMatchers.add(new AntPathRequestMatcher(basePath + "/"));

        return new AndRequestMatcher(new OrRequestMatcher(antMatchers), forPort(managementPort));
    }

    private RequestMatcher forPort(final int port) {
        return (HttpServletRequest request) -> port == request.getLocalPort();
    }
}
