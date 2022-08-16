package it.smartcommunitylab.aac.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
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

    /*
     * Configure a separated security context for management
     */
    @Bean("monitoringSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // match only actuator endpoints
        http.requestMatcher(forPort(managementPort))
                .authorizeRequests((requests) -> requests.anyRequest().permitAll())
                // we don't want a session for these endpoints
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

    private RequestMatcher forPort(final int port) {
        return (HttpServletRequest request) -> port == request.getLocalPort();
    }

}
