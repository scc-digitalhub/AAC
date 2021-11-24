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
import org.springframework.context.annotation.Primary;
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
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.internal.InternalConfirmKeyAuthenticationFilter;
import it.smartcommunitylab.aac.internal.InternalLoginAuthenticationFilter;
import it.smartcommunitylab.aac.internal.InternalResetKeyAuthenticationFilter;
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
@Order(19)
@EnableConfigurationProperties
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("classpath:/testdata.yml")
    private Resource dataMapping;

    @Value("${application.url}")
    private String applicationURL;

    private String loginPath = "/login";
    private String logoutPath = "/logout";

//    @Autowired
//    OAuth2ClientContext oauth2ClientContext;

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
    private ProviderRepository<InternalIdentityProviderConfig> internalProviderRepository;

    @Autowired
    private ProviderRepository<OIDCIdentityProviderConfig> oidcProviderRepository;

    @Autowired
    private ProviderRepository<SamlIdentityProviderConfig> samlProviderRepository;

    @Autowired
    private ProviderRepository<SpidIdentityProviderConfig> spidProviderRepository;

//    @Autowired
//    private UserRepository userRepository;

//    @Autowired
//    private ClientDetailsRepository clientDetailsRepository;

//    @Autowired
//    private UserDetailsService userDetailsService;
//
//    @Autowired
//    private TokenStore tokenStore;
//
//    @Autowired
//    private OAuth2ClientDetailsProvider oauth2ClientDetailsProvider;

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

//    // external oauth providers filter
//    // TODO move configuration, cleanup
//    @Bean
//    @ConfigurationProperties("oauth-providers")
//    public OAuthProviders oauthProviders() {
//        return new OAuthProviders();
//    }
//
//    // TODO replace with filterRegistrationBean and explicitely map urls
//    private Filter extOAuth2Filter() throws Exception {
//        CompositeFilter filter = new CompositeFilter();
//        List<Filter> filters = new ArrayList<>();
//        List<ClientResources> providers = oauthProviders().getProviders();
//        for (ClientResources client : providers) {
//            String id = client.getProvider();
//            filters.add(extOAuth2Filter(client, Utils.filterRedirectURL(id), "/eauth/" + id));
//        }
//        filter.setFilters(filters);
//        return filter;
//
//    }
//
//    private Filter extOAuth2Filter(ClientResources client, String path, String target) throws Exception {
//        OAuth2ClientAuthenticationProcessingFilter filter = new MultitenantOAuth2ClientAuthenticationProcessingFilter(
//                client.getProvider(), path, oauth2ClientDetailsProvider);
//
//        Yaml yaml = new Yaml();
//        MockDataMappings data = yaml.loadAs(dataMapping.getInputStream(), MockDataMappings.class);
//
//        filter.setAuthenticationSuccessHandler(new MockDataAwareOAuth2SuccessHandler(target, data));
//
//        OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
//        filter.setRestTemplate(template);
//        UserInfoTokenServices tokenServices = new UserInfoTokenServices(client.getResource().getUserInfoUri(),
//                client.getClient().getClientId());
//        tokenServices.setRestTemplate(template);
//        filter.setTokenServices(tokenServices);
//        return filter;
//    }

//	@Override
//	public void configure(WebSecurity web) throws Exception {
//	    web.ignoring()
//	    .antMatchers("/eauth/authorize/**");
//	}

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
                // whitelist public
                .antMatchers("/.well-known/**").permitAll()
                .antMatchers("/jwk").permitAll()
                // whitelist assets
                // TODO change path to /assets (and build)
                .antMatchers("/svg/**").permitAll()
                .antMatchers("/css/**").permitAll()
                .antMatchers("/img/**").permitAll()
                .antMatchers("/italia/**").permitAll()
                .antMatchers("/spid/**").permitAll()
                .antMatchers("/favicon.ico").permitAll()
                // whitelist docs
                .antMatchers("/docs/**").permitAll()
                // whitelist swagger
                .antMatchers(
                		"/v3/api-docs",
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

//        http
////		.anonymous().disable()
//                .authorizeRequests()
////                .antMatchers("/eauth/authorize/**").permitAll()
////                .antMatchers("/oauth/authorize", "/eauth/**").permitAll()
////                .antMatchers("/oauth/authorize", "/eauth/**").authenticated()
//                .antMatchers("/", "/dev**", "/account/**")
//                .hasAnyAuthority((restrictedAccess ? "ROLE_MANAGER" : "ROLE_USER"), "ROLE_ADMIN")
//                .antMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN")
//                .and()
//                .exceptionHandling()
//                //use a realm aware entryPoint
//                .authenticationEntryPoint(new RealmAwareAuthenticationEntryPoint("/login"))
//                .accessDeniedPage("/accesserror")
//                .and()
//                .logout()
//                .logoutSuccessHandler(logoutSuccessHandler()).permitAll()
////                .and()
////                .rememberMe()
////                .key(remembermeKey)
////                .rememberMeServices(rememberMeServices())
//                .and()
//                .csrf()
//                .disable()
////                // TODO replace with filterRegistrationBean and explicitely map urls
//                .addFilterBefore(getSamlAuthorityFilters(authManager, relyingPartyRegistrationRepository),
//                        BasicAuthenticationFilter.class)
//                .addFilterBefore(getOIDCAuthorityFilters(authManager, clientRegistrationRepository),
//                        BasicAuthenticationFilter.class);

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

    @Bean
    @Override
    @Primary
    public AuthenticationManager authenticationManagerBean() throws Exception {
//        return extendedAuthenticationManager();
        return authManager;
    }

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
            ProviderRepository<InternalIdentityProviderConfig> providerRepository,
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
            ProviderRepository<OIDCIdentityProviderConfig> providerRepository,
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
            ProviderRepository<SamlIdentityProviderConfig> providerRepository,
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
            ProviderRepository<SpidIdentityProviderConfig> providerRepository,
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

//    @Bean
//    public ClientDetailsUserDetailsService clientDetailsUserDetailsService(ClientDetailsService clientDetailsService) {
//       return new ClientDetailsUserDetailsService(clientDetailsService);                
//    }

//    public DaoAuthenticationProvider clientAuthProvider(ClientDetailsService clientDetailsService) {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(new ClientDetailsUserDetailsService(clientDetailsService));
//        authProvider.setPasswordEncoder(PlaintextPasswordEncoder.getInstance());
//        return authProvider;
//    }

//    // TODO customize authenticationprovider to handle per realm sessions
//    @Bean
//    @Override
//    public AuthenticationManager authenticationManagerBean() throws Exception {
//        return super.authenticationManagerBean();
//    }
//
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
////        auth.userDetailsService(userDetailsService);
//        auth.authenticationProvider(internalAuthProvider());
//    }

//    @Autowired
//    private InternalUserAccountRepository userRepository;
//    @Autowired
//    private RoleEntityRepository roleRepository;

//    public InternalUserDetailsService userDetailsService() {
//        return new InternalUserDetailsService(userRepository, roleRepository);
//    }
//
//    // internal authentication provider
//    // TODO use singleton
//    @Bean
//    public InternalAuthenticationProvider internalAuthProvider() throws Exception {
//        InternalAuthenticationProvider provider = new InternalAuthenticationProvider(userDetailsService());
//        provider.setPasswordEncoder(getInternalPasswordEncoder());
//        return provider;
//    }
//
//    @Bean
//    public InternalSubjectResolver internalSubjectResolver() throws Exception {
//        InternalSubjectResolver resolver = new InternalSubjectResolver(userRepository);
//        return resolver;
//    }

//    // TODO rework after integrating authorization server from lib
//    @Bean
//    protected ContextExtender contextExtender() {
//        return new ContextExtender(applicationURL, configMatchPorts, configMatchSubDomains);
//    }

//    // TODO move to oauth configurer
//    @Configuration
//    @EnableAuthorizationServer
//    protected class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
//
//        @Value("${oauth2.authcode.validity}")
//        private int authCodeValidity;
//
//        @Value("${oauth2.pkce.allowRefresh}")
//        private boolean oauth2PKCEAllowRefresh;
//
//        @Value("${oauth2.clientCredentials.allowRefresh}")
//        private boolean oauth2ClientCredentialsAllowRefresh;
//
//        @Value("${oauth2.resourceOwnerPassword.allowRefresh}")
//        private boolean oauth2ResourceOwnerPasswordAllowRefresh;
//
//        @Autowired
//        private DataSource dataSource;
//
//        @Autowired
//        private ClientDetailsService clientDetailsService;
//
//        @Autowired
//        private ApprovalStore approvalStore;
//
//        @Autowired
//        private UserApprovalHandler userApprovalHandler;
//
//        @Autowired
//        private AuthenticationManager authenticationManager;
//
//        @Autowired
//        private ClientDetailsRepository clientDetailsRepository;
//
//        @Autowired
//        @Qualifier("appTokenServices")
//        private AuthorizationServerTokenServices resourceServerTokenServices;
//
//        @Autowired
//        private AutoJdbcAuthorizationCodeServices authorizationCodeServices;
//
//        @Bean
//        public AutoJdbcAuthorizationCodeServices getAuthorizationCodeServices() throws PropertyVetoException {
//            return new AutoJdbcAuthorizationCodeServices(dataSource, authCodeValidity);
//        }
//
//        @Bean
//        public OAuth2RequestFactory getOAuth2RequestFactory() throws PropertyVetoException {
//            AACOAuth2RequestFactory<UserManager> result = new AACOAuth2RequestFactory<>();
//            return result;
//
//        }
//
//        @Bean
//        public OAuthFlowExtensions getFlowExtensions() throws PropertyVetoException {
//            return new WebhookOAuthFlowExtensions();
//        }
//
//        @Bean
//        public UserApprovalHandler getUserApprovalHandler() throws PropertyVetoException {
//            UserApprovalHandler bean = new UserApprovalHandler();
//            bean.setApprovalStore(approvalStore);
//            bean.setClientDetailsService(clientDetailsService);
//            bean.setRequestFactory(getOAuth2RequestFactory());
//            bean.setFlowExtensions(getFlowExtensions());
//            return bean;
//        }
//
//        private Filter endpointFilter() {
//            ClientCredentialsTokenEndpointFilter filter = new ClientCredentialsTokenEndpointFilter(
//                    clientDetailsRepository);
//            filter.setAuthenticationManager(authenticationManager);
//            // need to initialize success/failure handlers
//            filter.afterPropertiesSet();
//            return filter;
//        }
//
//        @Override
//        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
//            clients.withClientDetails(clientDetailsService);
//        }
//
//        @Override
//        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
//            endpoints.tokenStore(tokenStore).userApprovalHandler(userApprovalHandler)
//                    .authenticationManager(authenticationManager)
//                    .requestFactory(getOAuth2RequestFactory())
//                    .requestValidator(new AACOAuth2RequestValidator())
//                    .tokenServices(resourceServerTokenServices)
//                    .authorizationCodeServices(authorizationCodeServices)
//                    // set tokenGranter now to ensure all services are set
//                    .tokenGranter(tokenGranter(endpoints))
//                    .exceptionTranslator(new AACWebResponseExceptionTranslator());
//        }
//
//        @Override
//        public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
//            oauthServer.addTokenEndpointAuthenticationFilter(endpointFilter());
//            // disable default endpoints: we enable access
//            // because the endpoints are mapped to out custom controller returning 404
//            // TODO rework after integrating authorization server from lib
//            oauthServer.tokenKeyAccess("permitAll()");
//            oauthServer.checkTokenAccess("permitAll()");
//        }
//
//        private TokenGranter tokenGranter(final AuthorizationServerEndpointsConfigurer endpoints) {
//            // build our own list of granters
//            List<TokenGranter> granters = new ArrayList<TokenGranter>();
//            // insert PKCE auth code granter as the first one to supersede basic authcode
//            PKCEAwareTokenGranter pkceTokenGranter = new PKCEAwareTokenGranter(endpoints.getTokenServices(),
//                    authorizationCodeServices,
//                    endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory());
//            if (oauth2PKCEAllowRefresh) {
//                pkceTokenGranter.setAllowRefresh(true);
//            }
//            granters.add(pkceTokenGranter);
//
//            // auth code
//            granters.add(new AuthorizationCodeTokenGranter(endpoints.getTokenServices(),
//                    endpoints.getAuthorizationCodeServices(), endpoints.getClientDetailsService(),
//                    endpoints.getOAuth2RequestFactory()));
//
//            // refresh
//            granters.add(new RefreshTokenGranter(endpoints.getTokenServices(), endpoints.getClientDetailsService(),
//                    endpoints.getOAuth2RequestFactory()));
//
//            // implicit
//            granters.add(new ImplicitTokenGranter(endpoints.getTokenServices(),
//                    endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));
//
//            // client credentials
//            ClientCredentialsTokenGranter clientCredentialsTokenGranter = new ClientCredentialsTokenGranter(
//                    endpoints.getTokenServices(),
//                    endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory());
//            if (oauth2ClientCredentialsAllowRefresh) {
//                clientCredentialsTokenGranter.setAllowRefresh(true);
//            }
//            granters.add(clientCredentialsTokenGranter);
//
//            // resource owner password
//            if (authenticationManager != null) {
//                ResourceOwnerPasswordTokenGranter passwordTokenGranter = new ResourceOwnerPasswordTokenGranter(
//                        authenticationManager, endpoints.getTokenServices(),
//                        endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory());
//                if (!oauth2ResourceOwnerPasswordAllowRefresh) {
//                    passwordTokenGranter.setAllowRefresh(false);
//                }
//                granters.add(passwordTokenGranter);
//            }
//
//            return new CompositeTokenGranter(granters);
//        }
//    }

//    /*
//     * API resources : user
//     */
//
//    @Bean
//    protected ResourceServerConfiguration profileResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher("/*profile/**").authorizeRequests()
//                        .antMatchers(HttpMethod.OPTIONS, "/*profile/**").permitAll()
//                        .antMatchers("/basicprofile/all/{{\\w+}}")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_BASIC_PROFILE_ALL + "')")
//                        .antMatchers("/basicprofile/all")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_BASIC_PROFILE_ALL + "')")
//                        .antMatchers("/basicprofile/profiles")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_BASIC_PROFILE_ALL + "')")
//                        .antMatchers("/basicprofile/me")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_BASIC_PROFILE + "')")
//                        .antMatchers("/accountprofile/profiles")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_ACCOUNT_PROFILE_ALL + "')")
//                        .antMatchers("/accountprofile/me")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_ACCOUNT_PROFILE + "')")
//                        .and().csrf().disable();
//            }
//        }));
//        resource.setOrder(4);
//        return resource;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration restRegistrationResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            Filter endpointFilter() throws Exception {
//                ClientCredentialsRegistrationFilter filter = new ClientCredentialsRegistrationFilter(
//                        clientDetailsRepository);
//                filter.setFilterProcessesUrl("/internal/register/rest");
//                filter.setAuthenticationManager(authenticationManagerBean());
//                // need to initialize success/failure handlers
//                filter.afterPropertiesSet();
//                return filter;
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.addFilterAfter(endpointFilter(), BasicAuthenticationFilter.class);
//
//                http.antMatcher("/internal/register/rest").authorizeRequests().anyRequest()
//                        .fullyAuthenticated().and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(5);
//        return resource;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration rolesResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher("/*userroles/**").authorizeRequests()
//                        .antMatchers(HttpMethod.OPTIONS, "/*userroles/**").permitAll()
//                        .antMatchers("/userroles/me").access("#oauth2.hasScope('" + Config.SCOPE_ROLE + "')")
//                        .antMatchers(HttpMethod.GET, "/userroles/role")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_ROLES_READ + "')")
//                        .antMatchers(HttpMethod.GET, "/userroles/user/{\\w+}")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_ROLES_READ + "')")
//                        .antMatchers(HttpMethod.PUT, "/userroles/user/{\\w+}")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_ROLES_WRITE + "')")
//                        .antMatchers(HttpMethod.DELETE, "/userroles/user/{\\w+}")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_ROLES_WRITE + "')")
//                        .antMatchers("/userroles/client/{\\w+}")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_CLIENT_ROLES_READ_ALL + "')")
//                        .antMatchers("/userroles/client")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_CLIENT_ROLES_READ_ALL + "')")
//                        .antMatchers("/userroles/token/{\\w+}")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_CLIENT_ROLES_READ_ALL + "')")
//                        .and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(6);
//        return resource;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration authorizationResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher("/*authorization/**").authorizeRequests()
//                        .antMatchers(HttpMethod.OPTIONS, "/*authorization/**")
//                        .permitAll()
//                        .antMatchers("/authorization/**").access("#oauth2.hasScope('" + Config.SCOPE_AUTH_MANAGE + "')")
//                        .antMatchers("/authorization/*/schema/**")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_AUTH_SCHEMA_MANAGE + "')")
//                        .and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(9);
//        return resource;
//    }
//
//    /*
//     * OPENID API Resources: User
//     */
//
//    @Bean
//    protected ResourceServerConfiguration userInfoResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher(UserInfoEndpoint.USERINFO_URL).authorizeRequests()
//                        .antMatchers(HttpMethod.OPTIONS, UserInfoEndpoint.USERINFO_URL).permitAll()
//                        .antMatchers(UserInfoEndpoint.USERINFO_URL)
//                        .access("#oauth2.hasScope('" + Config.SCOPE_OPENID + "')")
//                        .and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(11);
//        return resource;
//    }
//
//    /*
//     * OAUTH2 API Resources : Client
//     */
//
//    // client auth provider uses plaintext passwords
//    public DaoAuthenticationProvider clientAuthProvider() {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(new OAuthClientUserDetails(clientDetailsRepository));
//        authProvider.setPasswordEncoder(PlaintextPasswordEncoder.getInstance());
//        return authProvider;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration tokenIntrospectionResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher(TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL).authorizeRequests()
//                        .antMatchers(TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL)
//                        .hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_TRUSTED")
//                        .and().httpBasic()
//                        .and().authenticationProvider(clientAuthProvider());
//            }
//        }));
//        resource.setOrder(12);
//        return resource;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration tokenRevocationResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher(TokenRevocationEndpoint.TOKEN_REVOCATION_URL).authorizeRequests()
//                        .antMatchers(TokenRevocationEndpoint.TOKEN_REVOCATION_URL)
//                        .hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_TRUSTED")
//                        .and().httpBasic()
//                        .and().authenticationProvider(clientAuthProvider());
//            }
//        }));
//        resource.setOrder(13);
//        return resource;
//    }
//
//    /*
//     * API Resources : services and claims
//     */
//
//    @Bean
//    protected ResourceServerConfiguration serviceManagementResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.regexMatcher("/api/services(.)*").authorizeRequests()
//                        .regexMatchers(HttpMethod.OPTIONS, "/api/services(.)*")
//                        .permitAll()
//                        .regexMatchers("/api/services(.)*")
//                        .access("#oauth2.hasAnyScope('" + Config.SCOPE_SERVICEMANAGEMENT + "', '"
//                                + Config.SCOPE_SERVICEMANAGEMENT_USER + "')")
//                        .and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(14);
//        return resource;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration claimManagementResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher("/api/claims/**").authorizeRequests().antMatchers(HttpMethod.OPTIONS, "/api/claims/**")
//                        .permitAll()
//                        .antMatchers("/api/claims/**")
//                        .access("#oauth2.hasAnyScope('" + Config.SCOPE_CLAIMMANAGEMENT + "', '"
//                                + Config.SCOPE_CLAIMMANAGEMENT_USER + "')")
//                        .and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(15);
//        return resource;
//    }
//
//    /*
//     * APIkey
//     */
//
//    @Bean
//    protected ResourceServerConfiguration apiKeyResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.regexMatcher("/apikeycheck(.*)").authorizeRequests()
//                        .regexMatchers("/apikeycheck(.*)").hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_TRUSTED")
//                        .and().httpBasic()
//                        .and().authenticationProvider(clientAuthProvider());
//
//                http.csrf().disable();
//            }
//
//        }));
//        resource.setOrder(16);
//        return resource;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration apiKeyClientResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.regexMatcher("/apikey/client(.*)").authorizeRequests()
//                        .antMatchers("/apikey/client/me")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_APIKEY_CLIENT + "')")
//                        .antMatchers("/apikey/client/{\\w+}")
//                        .access("#oauth2.hasAnyScope('" + Config.SCOPE_APIKEY_CLIENT + "','"
//                                + Config.SCOPE_APIKEY_CLIENT_ALL + "')")
//                        .antMatchers(HttpMethod.POST, "/apikey/client")
//                        .access("#oauth2.hasAnyScope('" + Config.SCOPE_APIKEY_CLIENT + "','"
//                                + Config.SCOPE_APIKEY_CLIENT_ALL + "')")
//                        .and().userDetailsService(new OAuthClientUserDetails(clientDetailsRepository));
//
//                http.csrf().disable();
//            }
//
//        }));
//        resource.setOrder(17);
//        return resource;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration apiKeyUserResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.regexMatcher("/apikey/user(.*)").authorizeRequests()
//                        .antMatchers("/apikey/user/me").access("#oauth2.hasScope('" + Config.SCOPE_APIKEY_USER + "')")
//                        .antMatchers("/apikey/user/{\\w+}")
//                        .access("#oauth2.hasScope('" + Config.SCOPE_APIKEY_USER + "')")
//                        .antMatchers(HttpMethod.POST, "/apikey/user")
//                        .access("#oauth2.hasAnyScope('" + Config.SCOPE_APIKEY_USER + "','"
//                                + Config.SCOPE_APIKEY_USER_CLIENT + "')")
//                        .and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(18);
//        return resource;
//    }
//
//    /*
//     * WSO2 integration
//     */
//    @Bean
//    protected ResourceServerConfiguration wso2ClientResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher("/wso2/client/**").authorizeRequests().anyRequest()
//                        .access("#oauth2.hasScope('" + Config.SCOPE_CLIENTMANAGEMENT + "')").and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(7);
//        return resource;
//    }
//
//    @Bean
//    protected ResourceServerConfiguration wso2APIResources() {
//        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
//            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
//                super.setConfigurers(configurers);
//            }
//        };
//        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
//            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//                resources.resourceId(null);
//            }
//
//            public void configure(HttpSecurity http) throws Exception {
//                http.antMatcher("/wso2/resources/**").authorizeRequests().anyRequest()
//                        .access("#oauth2.hasScope('" + Config.SCOPE_APIMANAGEMENT + "')").and().csrf().disable();
//            }
//
//        }));
//        resource.setOrder(8);
//        return resource;
//    }

}
