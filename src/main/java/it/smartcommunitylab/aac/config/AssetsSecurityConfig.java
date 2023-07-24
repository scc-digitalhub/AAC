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
 * Security policy for public (static) assets
 *
 * TODO build assets
 */
@Configuration
@Order(29)
public class AssetsSecurityConfig {

    /*
     * Configure a separated security context for static assets
     *
     */
    @Order(29)
    @Bean("assetsSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only endpoints
        http
            .requestMatcher(getRequestMatcher())
            // public access
            .authorizeRequests(requests -> requests.anyRequest().permitAll())
            // disable csrf, we expose only GET
            .csrf()
            .disable()
            // we don't want a session for these endpoints
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays
            .stream(ASSETS_URLS)
            .map(u -> new AntPathRequestMatcher(u))
            .collect(Collectors.toList());

        return new OrRequestMatcher(antMatchers);
    }

    public static final String[] ASSETS_URLS = {
        // TODO change path to /assets
        "/svg/**",
        "/css/**",
        "/img/**",
        "/italia/**",
        "/logo",
        "/favicon.ico",
    };
}
