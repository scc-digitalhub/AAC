package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.oauth.auth.InternalOpaqueTokenIntrospector;
import it.smartcommunitylab.aac.oauth.endpoint.ClientRegistrationEndpoint;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/*
 * Security context for OAuth2/OIDC Dynamic client registration
 *
 * Builds a stateless context with JWT/OAuth2 auth.
 * We actually use Bearer tokens and validate by fetching tokens from store
 */

@Configuration
@Order(23)
public class OAuth2DCRSecurityConfig {

    @Autowired
    private InternalOpaqueTokenIntrospector tokenIntrospector;

    /*
     * Configure a separated security context for API
     */
    @Order(23)
    @Bean("oauth2DCRSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // match only dcr endpoints, do not require any role since we support open
        // registration as anonymous
        http
            .requestMatcher(getRequestMatcher())
            .authorizeRequests(authorizeRequests ->
                authorizeRequests.anyRequest().hasAnyAuthority(Config.R_CLIENT, Config.R_USER, "ROLE_ANONYMOUS")
            )
            // bare authentication manager with only anonymous+bearer
            .authenticationManager(
                new ProviderManager(
                    new AnonymousAuthenticationProvider(UUID.randomUUID().toString()),
                    new OpaqueTokenAuthenticationProvider(tokenIntrospector)
                )
            )
            // use bearer token auth, will add filter
            .oauth2ResourceServer(oauth2 ->
                oauth2.opaqueToken(opaqueToken -> opaqueToken.introspector(tokenIntrospector))
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

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays
            .stream(OAUTH2_DCR_URLS)
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

    private static final String DCR_REGISTRATION_URL = ClientRegistrationEndpoint.REGISTRATION_URL;
    private static final String DCR_MANAGE_URL = ClientRegistrationEndpoint.REGISTRATION_URL + "/**";

    private static final String[] OAUTH2_DCR_URLS = { DCR_REGISTRATION_URL, DCR_MANAGE_URL };
}
