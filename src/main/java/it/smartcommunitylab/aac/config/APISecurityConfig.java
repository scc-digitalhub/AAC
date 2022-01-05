package it.smartcommunitylab.aac.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import it.smartcommunitylab.aac.oauth.auth.InternalOpaqueTokenIntrospector;

/*
 * Security context for API endpoints
 * 
 * Builds a stateless context with JWT/OAuth2 auth.
 * We actually use Bearer tokens and validate by fetching tokens from store
 */

@Configuration
@Order(14)
public class APISecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private InternalOpaqueTokenIntrospector tokenIntrospector;

    /*
     * Configure a separated security context for API
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // match only token endpoints
        http.requestMatcher(getRequestMatcher())
                .authorizeRequests((authorizeRequests) -> authorizeRequests
                        .anyRequest().hasAnyAuthority("ROLE_USER", "ROLE_CLIENT"))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .opaqueToken(opaqueToken -> opaqueToken
                                .introspector(tokenIntrospector)))
                // disable request cache, we override redirects but still better enforce it
                .requestCache((requestCache) -> requestCache.disable())
                .exceptionHandling()
                // use 403
                .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
                .accessDeniedPage("/accesserror")
                .and()
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .csrf()
                .disable();

        // TODO add bearer auth filter

        // we don't want a session for these endpoints, each request should be evaluated
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    public RequestMatcher getRequestMatcher() {
        return new OrRequestMatcher(
                new AntPathRequestMatcher(API_PREFIX + "/**"),
                new AntPathRequestMatcher("/scim/**"),
                new AntPathRequestMatcher("/profile/**"),
                new AntPathRequestMatcher("/roles/**"),
                // TODO remove legacy paths
                new AntPathRequestMatcher("/basicprofile/**"),
                new AntPathRequestMatcher("/accountprofile/**"),
                new AntPathRequestMatcher("/openidprofile/**"),
                new AntPathRequestMatcher("/userroles/me"),
                new AntPathRequestMatcher("/clientroles/me"));

    }

    public static final String API_PREFIX = "/api";

}
