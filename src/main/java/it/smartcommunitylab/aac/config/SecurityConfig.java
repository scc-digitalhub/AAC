/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.CompositeFilter;
import it.smartcommunitylab.aac.core.ExtendedUserAuthenticationManager;
import it.smartcommunitylab.aac.core.auth.ExtendedLogoutSuccessHandler;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.internal.auth.InternalConfirmKeyAuthenticationFilter;
import it.smartcommunitylab.aac.internal.auth.InternalLoginAuthenticationFilter;
import it.smartcommunitylab.aac.internal.auth.InternalResetKeyAuthenticationFilter;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.oauth.auth.AuthorizationEndpointFilter;
import it.smartcommunitylab.aac.oauth.auth.OAuth2RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.auth.OIDCLoginAuthenticationFilter;
import it.smartcommunitylab.aac.openid.auth.OIDCRedirectAuthenticationFilter;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SamlMetadataFilter;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationFilter;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.spid.auth.SpidMetadataFilter;
import it.smartcommunitylab.aac.spid.auth.SpidWebSsoAuthenticationFilter;
import it.smartcommunitylab.aac.spid.auth.SpidWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;

/*
 * Security config for AAC UI
 * 
 * Should be after all api/endpoint config to works as catch-all for remaining requests
 */

@Configuration
@Order(29)
@EnableConfigurationProperties
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("classpath:/testdata.yml")
    private Resource dataMapping;

    @Value("${application.url}")
    private String applicationURL;

    private final String loginPath = "/login";
    private final String logoutPath = "/logout";

    @Autowired
    private OIDCClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    @Qualifier("samlRelyingPartyRegistrationRepository")
    private SamlRelyingPartyRegistrationRepository samlRelyingPartyRegistrationRepository;

    @Autowired
    @Qualifier("spidRelyingPartyRegistrationRepository")
    private SamlRelyingPartyRegistrationRepository spidRelyingPartyRegistrationRepository;

    @Autowired
    private OAuth2ClientDetailsService oauth2ClientDetailsService;

    @Autowired
    private OAuth2ClientService oauth2ClientService;

    @Autowired
    private InternalUserAccountService internalUserAccountService;

//    @Autowired
//    private OAuth2ClientUserDetailsService clientUserDetailsService;

    @Autowired
    private ExtendedUserAuthenticationManager authManager;

    @Autowired
    private RealmAwarePathUriBuilder realmUriBuilder;

    @Autowired
    private ProviderConfigRepository<InternalIdentityProviderConfig> internalProviderRepository;

    @Autowired
    private ProviderConfigRepository<OIDCIdentityProviderConfig> oidcProviderRepository;

    @Autowired
    private ProviderConfigRepository<SamlIdentityProviderConfig> samlProviderRepository;

    @Autowired
    private ProviderConfigRepository<SpidIdentityProviderConfig> spidProviderRepository;

//    /*
//     * rememberme
//     */
//    @Bean
//    public PersistentTokenBasedRememberMeServices rememberMeServices() {
//        AACRememberMeServices service = new AACRememberMeServices(remembermeKey, new UserDetailsRepo(userRepository),
//                persistentTokenRepository());
//        service.setCookieName(Config.COOKIE_REMEMBER_ME);
//        service.setParameter(Config.PARAM_REMEMBER_ME);
//        service.setTokenValiditySeconds(3600 * 24 * 60); // two month
//        return service;
//    }
//
//    @Bean
//    public PersistentTokenRepository persistentTokenRepository() {
//        JdbcTokenRepositoryImpl tokenRepositoryImpl = new JdbcTokenRepositoryImpl();
//        tokenRepositoryImpl.setDataSource(dataSource);
//        return tokenRepositoryImpl;
//    }

    // disabled for upgrade, TODO fix cors
//	@Bean
//	public FilterRegistrationBean corsFilter() {
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		CorsConfiguration config = new CorsConfiguration();
//		config.setAllowCredentials(true);
//		config.addAllowedOrigin("*");
//		config.addAllowedHeader("*");
//		config.addAllowedMethod("*");
//		source.registerCorsConfiguration("/**", config);
//		FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
//		bean.setOrder(0);
//		return bean;
//	}

    @Bean
    public InternalPasswordEncoder getInternalPasswordEncoder() {
        return new InternalPasswordEncoder();
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {

        http
                .authorizeRequests()
                // whitelist error
                .antMatchers("/error").permitAll()
                // whitelist login pages
                .antMatchers(loginPath, logoutPath).permitAll()
                .antMatchers("/-/{realm}/" + loginPath).permitAll()
                .antMatchers("/endsession").permitAll()
                // whitelist auth providers pages (login,registration etc)
                .antMatchers("/auth/**").permitAll()
//                // whitelist public
//                .antMatchers("/.well-known/**").permitAll()
//                .antMatchers("/jwk").permitAll()
                // whitelist assets
                // TODO change path to /assets (and build)
                .antMatchers("/svg/**").permitAll()
                .antMatchers("/css/**").permitAll()
                .antMatchers("/img/**").permitAll()
                .antMatchers("/italia/**").permitAll()
                .antMatchers("/spid/**").permitAll()
                .antMatchers("/favicon.ico").permitAll()
                // whitelist swagger
                .antMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs.yaml",
                        "/v3/api-docs/*",
                        "/configuration/ui",
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/configuration/**",
                        "/swagger-ui.html",
                        "/webjars/**")
                .permitAll()
                // anything else requires auth
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                // use a realm aware entryPoint
//                .authenticationEntryPoint(new RealmAwareAuthenticationEntryPoint("/login"))
                .defaultAuthenticationEntryPointFor(
                        oauth2AuthenticationEntryPoint(oauth2ClientDetailsService, loginPath),
                        new AntPathRequestMatcher("/oauth/**"))
                .defaultAuthenticationEntryPointFor(
                        realmAuthEntryPoint(loginPath, realmUriBuilder),
                        new AntPathRequestMatcher("/**"))
//                .accessDeniedPage("/accesserror")
                .and()
                .logout(logout -> logout
                        .logoutUrl(logoutPath)
                        .logoutRequestMatcher(new AntPathRequestMatcher(logoutPath))
                        .logoutSuccessHandler(logoutSuccessHandler(realmUriBuilder)).permitAll())

//                .and()
//                .rememberMe()
//                .key(remembermeKey)
//                .rememberMeServices(rememberMeServices())
//                .and()
                .csrf()
//                .disable()
                .ignoringAntMatchers("/logout", "/console/**", "/account/**",
                        "/auth/oidc/**",
                        "/auth/saml/**",
                        "/auth/spid/**")
                .and()
//                .disable()
//                // TODO replace with filterRegistrationBean and explicitely map urls
                .addFilterBefore(
                        getInternalAuthorityFilters(authManager, internalProviderRepository,
                                internalUserAccountService),
                        BasicAuthenticationFilter.class)
                .addFilterBefore(
                        getSamlAuthorityFilters(authManager, samlProviderRepository,
                                samlRelyingPartyRegistrationRepository),
                        BasicAuthenticationFilter.class)
                .addFilterBefore(
                        getSpidAuthorityFilters(authManager, spidProviderRepository,
                                spidRelyingPartyRegistrationRepository),
                        BasicAuthenticationFilter.class)
                .addFilterBefore(
                        getOIDCAuthorityFilters(authManager, oidcProviderRepository, clientRegistrationRepository),
                        BasicAuthenticationFilter.class)
                .addFilterBefore(new AuthorizationEndpointFilter(oauth2ClientService, oauth2ClientDetailsService),
                        BasicAuthenticationFilter.class);
//                .addFilterBefore(new ExpiredUserAuthenticationFilter(), BasicAuthenticationFilter.class);

        // we always want a session here
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);

    }

    @Bean
    public CompositeLogoutHandler logoutHandler() {
        List<LogoutHandler> handlers = new ArrayList<>();
        SecurityContextLogoutHandler contextLogoutHandler = new SecurityContextLogoutHandler();
        contextLogoutHandler.setClearAuthentication(true);
        contextLogoutHandler.setInvalidateHttpSession(true);
        handlers.add(contextLogoutHandler);

        // cookie clearing
        String[] cookieNames = { "JSESSIONID", "csrftoken" };
        CookieClearingLogoutHandler cookieLogoutHandler = new CookieClearingLogoutHandler(cookieNames);
        handlers.add(cookieLogoutHandler);

        // TODO define tokenRepository as bean and use for csrf
//        CsrfLogoutHandler csrfLogoutHandler = new CsrfLogoutHandler(csrfTokenRepository);
//        handlers.add(csrfLogoutHandler);
        // TODO add remember me
        // localStorage clear - TODO add to httpSecurity handlers above
        LogoutHandler clearSiteLogoutHandler = new HeaderWriterLogoutHandler(
                new ClearSiteDataHeaderWriter(ClearSiteDataHeaderWriter.Directive.STORAGE));

        handlers.add(clearSiteLogoutHandler);
        return new CompositeLogoutHandler(handlers);

    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler(
            RealmAwarePathUriBuilder uriBuilder) {
        // TODO update dedicated, leverage OidcClientInitiatedLogoutSuccessHandler
        ExtendedLogoutSuccessHandler handler = new ExtendedLogoutSuccessHandler(loginPath);
        handler.setRealmUriBuilder(uriBuilder);
        handler.setDefaultTargetUrl("/");
        handler.setTargetUrlParameter("target");
        return handler;
    }

//    @Bean
//    @Override
//    @Primary
//    public AuthenticationManager authenticationManagerBean() throws Exception {
////        return extendedAuthenticationManager();
//        return authManager;
//    }

    private RealmAwareAuthenticationEntryPoint realmAuthEntryPoint(String loginPath,
            RealmAwarePathUriBuilder uriBuilder) {
        RealmAwareAuthenticationEntryPoint entryPoint = new RealmAwareAuthenticationEntryPoint(loginPath);
        entryPoint.setUseForward(false);
        entryPoint.setRealmUriBuilder(uriBuilder);

        return entryPoint;

    }

    private RequestAwareAuthenticationSuccessHandler successHandler() {
        return new RequestAwareAuthenticationSuccessHandler();
    }

    /*
     * Internal auth
     */

    public CompositeFilter getInternalAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<InternalIdentityProviderConfig> providerRepository,
            InternalUserAccountService userAccountService) {

        List<Filter> filters = new ArrayList<>();

        InternalLoginAuthenticationFilter loginFilter = new InternalLoginAuthenticationFilter(
                userAccountService, providerRepository);
        loginFilter.setAuthenticationManager(authManager);
        loginFilter.setAuthenticationSuccessHandler(successHandler());
        filters.add(loginFilter);

        InternalConfirmKeyAuthenticationFilter confirmKeyFilter = new InternalConfirmKeyAuthenticationFilter(
                userAccountService, providerRepository);
        confirmKeyFilter.setAuthenticationManager(authManager);
        confirmKeyFilter.setAuthenticationSuccessHandler(successHandler());

        filters.add(confirmKeyFilter);

        InternalResetKeyAuthenticationFilter resetKeyFilter = new InternalResetKeyAuthenticationFilter(
                userAccountService, providerRepository);
        resetKeyFilter.setAuthenticationManager(authManager);
        resetKeyFilter.setAuthenticationSuccessHandler(successHandler());
        filters.add(resetKeyFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * OIDC Auth
     */

    public CompositeFilter getOIDCAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<OIDCIdentityProviderConfig> providerRepository,
            OIDCClientRegistrationRepository clientRegistrationRepository) {
        // build filters bound to shared client + request repos
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();
        OIDCRedirectAuthenticationFilter redirectFilter = new OIDCRedirectAuthenticationFilter(
                providerRepository,
                clientRegistrationRepository);
        redirectFilter.setAuthorizationRequestRepository(authorizationRequestRepository);

        OIDCLoginAuthenticationFilter loginFilter = new OIDCLoginAuthenticationFilter(
                providerRepository,
                clientRegistrationRepository);
        loginFilter.setAuthorizationRequestRepository(authorizationRequestRepository);
        loginFilter.setAuthenticationManager(authManager);

        List<Filter> filters = new ArrayList<>();
        filters.add(loginFilter);
        filters.add(redirectFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * Saml2 Auth
     */

    public CompositeFilter getSamlAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<SamlIdentityProviderConfig> providerRepository,
            SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {

        // request repository
        Saml2AuthenticationRequestRepository<Saml2AuthenticationRequestContext> authenticationRequestRepository = new HttpSessionSaml2AuthenticationRequestRepository();

        // build filters
        SamlWebSsoAuthenticationRequestFilter requestFilter = new SamlWebSsoAuthenticationRequestFilter(
                providerRepository,
                relyingPartyRegistrationRepository);
        requestFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SamlWebSsoAuthenticationFilter ssoFilter = new SamlWebSsoAuthenticationFilter(
                providerRepository,
                relyingPartyRegistrationRepository);
        ssoFilter.setAuthenticationManager(authManager);
        ssoFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SamlMetadataFilter metadataFilter = new SamlMetadataFilter(relyingPartyRegistrationRepository);

        List<Filter> filters = new ArrayList<>();
        filters.add(metadataFilter);
        filters.add(requestFilter);
        filters.add(ssoFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * SPID Auth
     */

    public CompositeFilter getSpidAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<SpidIdentityProviderConfig> providerRepository,
            SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {

        // request repository
        Saml2AuthenticationRequestRepository<Saml2AuthenticationRequestContext> authenticationRequestRepository = new HttpSessionSaml2AuthenticationRequestRepository();

        // build filters
        SpidWebSsoAuthenticationRequestFilter requestFilter = new SpidWebSsoAuthenticationRequestFilter(
                providerRepository,
                relyingPartyRegistrationRepository);
        requestFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SpidWebSsoAuthenticationFilter ssoFilter = new SpidWebSsoAuthenticationFilter(
                providerRepository,
                relyingPartyRegistrationRepository);
        ssoFilter.setAuthenticationManager(authManager);
        ssoFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SpidMetadataFilter metadataFilter = new SpidMetadataFilter(providerRepository,
                relyingPartyRegistrationRepository);

        List<Filter> filters = new ArrayList<>();
        filters.add(metadataFilter);
        filters.add(requestFilter);
        filters.add(ssoFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * OAuth2
     */

    private OAuth2RealmAwareAuthenticationEntryPoint oauth2AuthenticationEntryPoint(
            OAuth2ClientDetailsService clientDetailsService, String loginUrl) {
        // TODO implement support for "common" realm, "global" realm shoud not be
        // available here
        // we need a way to discover ALL providers for a common login.. infeasible.
        // Maybe let existing sessions work, or ask only for matching realm?
        return new OAuth2RealmAwareAuthenticationEntryPoint(clientDetailsService, loginUrl);

    }

}
