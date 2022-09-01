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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import it.smartcommunitylab.aac.oauth.endpoint.OAuth2MetadataEndpoint;
import it.smartcommunitylab.aac.openid.endpoint.JWKSetPublishingEndpoint;
import it.smartcommunitylab.aac.openid.endpoint.OpenIDMetadataEndpoint;

/*
 * Security policy for well-known public endpoints
 * 
 */
@Configuration
@Order(27)
public class WKNSecurityConfig {

    /*
     * Configure a separated security context for WKN
     */
    @Order(27)
    @Bean("wknSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only endpoints
        http.requestMatcher(getRequestMatcher())
                // public access
                .authorizeRequests((requests) -> requests.anyRequest().permitAll())
                // allow cors
                .cors().configurationSource(corsConfigurationSource())
                .and()
                // disable csrf, we expose only GET
                .csrf().disable()
                // we don't want a session for these endpoints
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays.stream(WKN_URLS).map(u -> new AntPathRequestMatcher(u))
                .collect(Collectors.toList());

        return new OrRequestMatcher(antMatchers);

    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET"));
        config.setAllowedHeaders(Arrays.asList("authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    public static final String[] WKN_URLS = {
            OpenIDMetadataEndpoint.OPENID_CONFIGURATION_URL,
            OpenIDMetadataEndpoint.WEBFINGER_URL,
            OAuth2MetadataEndpoint.OAUTH2_CONFIGURATION_URL,
            JWKSetPublishingEndpoint.JWKS_URL
    };

}
