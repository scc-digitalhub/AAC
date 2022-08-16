package it.smartcommunitylab.aac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import it.smartcommunitylab.aac.core.auth.Http401UnauthorizedEntryPoint;

/*
 * Security context for console endpoints
 * 
 * Builds a stateful context with no auth entrypoint
 */

@Configuration
@Order(25)
public class ConsoleSecurityConfig {

    /*
     * Configure a separated security context for API
     */
    @Bean("consoleSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only console endpoints and require user access
        http.requestMatcher(getRequestMatcher())
                .authorizeRequests((authorizeRequests) -> authorizeRequests
                        .anyRequest().hasAnyAuthority("ROLE_USER"))
                // disable request cache, we override redirects but still better enforce it
                .requestCache((requestCache) -> requestCache.disable())
                .exceptionHandling()
                // use 401
                .authenticationEntryPoint(new Http401UnauthorizedEntryPoint())
                .accessDeniedPage("/accesserror")
                .and()
                .csrf()
                .disable()
                // we want a session for console
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);

        return http.build();
    }

    public RequestMatcher getRequestMatcher() {
        return new AntPathRequestMatcher(CONSOLE_PREFIX + "/**");

    }

    public static final String CONSOLE_PREFIX = "/console";

}
