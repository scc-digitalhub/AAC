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

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.CompositeFilter;
import org.yaml.snakeyaml.Yaml;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.ExtendedAuthenticationManager;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntityRepository;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.internal.provider.InternalAuthenticationProvider;
import it.smartcommunitylab.aac.internal.provider.InternalSubjectResolver;
import it.smartcommunitylab.aac.internal.service.InternalUserDetailsService;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.auth.OIDCLoginAuthenticationFilter;
import it.smartcommunitylab.aac.openid.auth.OIDCRedirectAuthenticationFilter;
import it.smartcommunitylab.aac.saml.auth.SamlMetadataFilter;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationFilter;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.utils.Utils;

@Configuration
//@EnableOAuth2Client
@EnableConfigurationProperties
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("classpath:/testdata.yml")
    private Resource dataMapping;

    @Value("${application.url}")
    private String applicationURL;

    @Value("${security.restricted}")
    private boolean restrictedAccess;

    @Value("${security.rememberme.key}")
    private String remembermeKey;


    @Bean
    @ConfigurationProperties(prefix = "providers")
    public ProvidersProperties globalProviders() {
        return new ProvidersProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "attributesets")
    public AttributeSetsProperties systemAttributeSets() {
        return new AttributeSetsProperties();
    }

//    @Autowired
//    OAuth2ClientContext oauth2ClientContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    private OIDCClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    @Autowired
    private UserEntityService userService;

    @Autowired
    private ExtendedAuthenticationManager authManager;

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
//		.anonymous().disable()
                .authorizeRequests()
                .antMatchers("/eauth/authorize/**").permitAll()
                .antMatchers("/oauth/authorize", "/eauth/**").authenticated()
                .antMatchers("/", "/dev**", "/account/**")
                .hasAnyAuthority((restrictedAccess ? "ROLE_MANAGER" : "ROLE_USER"), "ROLE_ADMIN")
                .antMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN")
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                .accessDeniedPage("/accesserror")
                .and()
                .logout()
                .logoutSuccessHandler(logoutSuccessHandler()).permitAll()
//                .and()
//                .rememberMe()
//                .key(remembermeKey)
//                .rememberMeServices(rememberMeServices())
                .and()
                .csrf()
                .disable()
//                // TODO replace with filterRegistrationBean and explicitely map urls
                .addFilterBefore(getSamlAuthorityFilters(authManager, relyingPartyRegistrationRepository),
                        BasicAuthenticationFilter.class)
                .addFilterBefore(getOIDCAuthorityFilters(authManager, clientRegistrationRepository),
                        BasicAuthenticationFilter.class);

    }

    /**
     * @return
     */
    private LogoutSuccessHandler logoutSuccessHandler() {
        SimpleUrlLogoutSuccessHandler handler = new SimpleUrlLogoutSuccessHandler();
        handler.setDefaultTargetUrl("/");
        handler.setTargetUrlParameter("target");
        return handler;
    }

    @Bean
    @Override
    @Primary
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return extendedAuthenticationManager();
    }

    @Bean
    public ExtendedAuthenticationManager extendedAuthenticationManager() throws Exception {
        return new ExtendedAuthenticationManager(authorityManager, userService);
    }

    /*
     * OIDC Auth
     */

    @Bean
    public OIDCClientRegistrationRepository clientRegistrationRepository() {
        return new OIDCClientRegistrationRepository();
    }

    public CompositeFilter getOIDCAuthorityFilters(AuthenticationManager authManager,
            OIDCClientRegistrationRepository clientRegistrationRepository) {
        // build filters bound to shared client + request repos
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();
        OIDCRedirectAuthenticationFilter redirectFilter = new OIDCRedirectAuthenticationFilter(
                clientRegistrationRepository);
        redirectFilter.setAuthorizationRequestRepository(authorizationRequestRepository);

        OIDCLoginAuthenticationFilter loginFilter = new OIDCLoginAuthenticationFilter(clientRegistrationRepository);
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

    @Bean
    public SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        return new SamlRelyingPartyRegistrationRepository();
    }

    public CompositeFilter getSamlAuthorityFilters(AuthenticationManager authManager,
            SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        // build filters
        SamlWebSsoAuthenticationRequestFilter requestFilter = new SamlWebSsoAuthenticationRequestFilter(
                relyingPartyRegistrationRepository);

        SamlWebSsoAuthenticationFilter ssoFilter = new SamlWebSsoAuthenticationFilter(
                relyingPartyRegistrationRepository);
        ssoFilter.setAuthenticationManager(authManager);

        SamlMetadataFilter metadataFilter = new SamlMetadataFilter(relyingPartyRegistrationRepository);

        List<Filter> filters = new ArrayList<>();
        filters.add(metadataFilter);
        filters.add(requestFilter);
        filters.add(ssoFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

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
