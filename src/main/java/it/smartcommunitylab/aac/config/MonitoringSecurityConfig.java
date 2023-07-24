package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
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
            .requestMatcher(getRequestMatcher())
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
