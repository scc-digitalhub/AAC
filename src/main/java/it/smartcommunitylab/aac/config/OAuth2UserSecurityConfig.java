package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.auth.ExtendedLoginUrlAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.LoginUrlRequestConverter;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.oauth.auth.AuthorizationEndpointFilter;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAwareLoginUrlConverter;
import it.smartcommunitylab.aac.oauth.auth.OAuth2IdpAwareLoginUrlConverter;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;

/*
 * Security context for oauth2 endpoints
 * 
 * Builds a stateless context with oauth2 Client Auth
 */

@Configuration
@Order(28)
public class OAuth2UserSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${application.url}")
    private String applicationUrl;

    private final String loginPath = "/login";

    @Autowired
    private OAuth2ClientService clientService;

    @Autowired
    private OAuth2ClientDetailsService clientDetailsService;

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    private IdentityProviderService idpProviderService;

    /*
     * Configure a separated security context for oauth2 tokenEndpoints
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // match only user endpoints
        http.requestMatcher(getRequestMatcher()).authorizeRequests().anyRequest().authenticated()
//                .authorizeRequests((authorizeRequests) -> authorizeRequests
//                        .anyRequest().hasAnyAuthority(Config.R_USER))
                .and().exceptionHandling()
                .authenticationEntryPoint(authEntryPoint(loginPath))
                .accessDeniedPage("/accesserror")
                .and().cors().configurationSource(corsConfigurationSource())
                .and().csrf()
                .and()
                .addFilterBefore(
                        getOAuth2UserFilters(authorityManager, idpProviderService, clientDetailsService, clientService,
                                loginPath),
                        BasicAuthenticationFilter.class);

        // we do want a valid user session for these endpoints
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
    }

    public Filter getOAuth2UserFilters(
            AuthorityManager authorityManager, IdentityProviderService providerService,
            OAuth2ClientDetailsService oauth2ClientDetailsService, OAuth2ClientService oauth2ClientService,
            String loginUrl) {

        AuthorizationEndpointFilter authorizationFilter = new AuthorizationEndpointFilter(oauth2ClientService,
                oauth2ClientDetailsService);
        LoginUrlRequestConverter clientAwareConverter = new OAuth2ClientAwareLoginUrlConverter(clientDetailsService,
                loginUrl);
        ExtendedLoginUrlAuthenticationEntryPoint entryPoint = new ExtendedLoginUrlAuthenticationEntryPoint(loginUrl,
                clientAwareConverter);
        authorizationFilter.setAuthenticationEntryPoint(entryPoint);
        return authorizationFilter;
    }

    private AuthenticationEntryPoint authEntryPoint(String loginUrl) {
        ExtendedLoginUrlAuthenticationEntryPoint entryPoint = new ExtendedLoginUrlAuthenticationEntryPoint(loginUrl);
        List<LoginUrlRequestConverter> converters = new ArrayList<>();
        LoginUrlRequestConverter idpAwareConverter = new OAuth2IdpAwareLoginUrlConverter(idpProviderService,
                authorityManager);
        LoginUrlRequestConverter clientAwareConverter = new OAuth2ClientAwareLoginUrlConverter(clientDetailsService,
                loginUrl);

        converters.add(idpAwareConverter);
        converters.add(clientAwareConverter);
        entryPoint.setConverters(converters);

        return entryPoint;
    }

//    private RealmAwareAuthenticationEntryPoint realmAuthEntryPoint(String loginPath,
//            RealmAwarePathUriBuilder uriBuilder) {
//        RealmAwareAuthenticationEntryPoint entryPoint = new RealmAwareAuthenticationEntryPoint(loginPath);
//        entryPoint.setUseForward(false);
//        entryPoint.setRealmUriBuilder(uriBuilder);
//
//        return entryPoint;
//
//    }
//
//    private OAuth2RealmAwareAuthenticationEntryPoint clientAwareAuthenticationEntryPoint(
//            OAuth2ClientDetailsService clientDetailsService, String loginUrl) {
//        // TODO implement support for "common" realm, "global" realm shoud not be
//        // available here
//        // we need a way to discover ALL providers for a common login.. infeasible.
//        // Maybe let existing sessions work, or ask only for matching realm?
//        return new OAuth2RealmAwareAuthenticationEntryPoint(clientDetailsService, loginUrl);
//    }

    public RequestMatcher getRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays.stream(OAUTH2_URLS).map(u -> new AntPathRequestMatcher(u))
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

    private static final String AUTHORIZATION_ENDPOINT = "/oauth/authorize";

    private static final String[] OAUTH2_URLS = {
            AUTHORIZATION_ENDPOINT
    };

}