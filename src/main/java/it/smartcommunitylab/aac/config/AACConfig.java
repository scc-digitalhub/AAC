package it.smartcommunitylab.aac.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.claims.DefaultClaimsService;
import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import it.smartcommunitylab.aac.core.provider.UserTranslator;
import it.smartcommunitylab.aac.core.service.CoreUserTranslator;
import it.smartcommunitylab.aac.core.service.UserService;

/*
 * AAC core config, all services should already be up and running now
 */
@Configuration
@Order(10)
public class AACConfig {

    @Value("${application.url}")
    private String applicationUrl;

    /*
     * Core aac should be bootstrapped before services, security etc
     */

//    @Autowired
//    private DataSource dataSource;

//    @Autowired
//    private AuthorityManager authorityManager;

    /*
     * provider manager depends on authorities + static config + datasource
     */

//    @Autowired
//    private ProviderManager providerManager;

    /*
     * authManager depends on provider + userService
     */
//    @Autowired
//    private SubjectService subjectService;
//
//    @Autowired
//    private UserEntityService userService;
//
//    @Bean
//    public ExtendedUserAuthenticationManager extendedAuthenticationManager() throws Exception {
//        return new ExtendedUserAuthenticationManager(authorityManager, userService, subjectService);
//    }
//
//    @Bean
//    public AuthenticationHelper authenticationHelper() {
//        return new DefaultSecurityContextAuthenticationHelper();
//    }

    /*
     * initialize the execution service here and then build claims service
     */

    @Bean
    public ClaimsService claimsService(ExtractorsRegistry extractorsRegistry,
            ScriptExecutionService executionService,
            UserService userService) {
        DefaultClaimsService service = new DefaultClaimsService(extractorsRegistry);
        service.setExecutionService(executionService);
        service.setUserService(userService);
        return service;

    }

    /*
     * Cross realm user translator
     */
    @Bean
    public UserTranslator userTranslator() {
        return new CoreUserTranslator();
    }

    /*
     * Entrypoint
     */
    // TODO make sure all filters use this bean to build urls..
    @Bean
    public RealmAwarePathUriBuilder realmUriBuilder() {
        return new RealmAwarePathUriBuilder(applicationUrl);
    }

    /*
     * Locale resolver
     */
    @Bean //
    public LocaleResolver localeResolver() {
        // use cookie resolver without default to fallback on accept language header
        CookieLocaleResolver localeResolver = new CookieLocaleResolver();
        return localeResolver;
    }

//    @Autowired
//    private UserRepository userRepository;
//
////    @Autowired
////    private ClientDetailsRepository clientDetailsRepository;
//
//    @Autowired
//    private DataSource dataSource;

//    @Autowired
//    private TokenStore tokenStore;
//
//    @Autowired
//    private OIDCTokenEnhancer tokenEnhancer;
//
//    @Autowired
//    private AACJwtTokenConverter tokenConverter;
//
//    /*
//     * OAuth
//     */
//    // TODO split specifics to dedicated configurer
//
//    @Bean
//    public AutoJdbcTokenStore getTokenStore() throws PropertyVetoException {
//        return new AutoJdbcTokenStore(dataSource);
//    }
//
//    @Bean
//    public JdbcApprovalStore getApprovalStore() throws PropertyVetoException {
//        return new JdbcApprovalStore(dataSource);
//    }
//
//    @Bean
//    @Primary
//    public JdbcClientDetailsService getClientDetails() throws PropertyVetoException {
//        JdbcClientDetailsService bean = new AACJDBCClientDetailsService(dataSource);
//        bean.setRowMapper(getClientDetailsRowMapper());
//        return bean;
//    }

//    @Bean
//    @Primary
//    public UserDetailsService getInternalUserDetailsService() {
//        return new InternalUserDetailsRepo();
//    }
//
//    @Bean
//    public ClientDetailsRowMapper getClientDetailsRowMapper() {
//        return new ClientDetailsRowMapper(userRepository);
//    }
//
//    @Bean
//    @Primary
//    public OAuth2ClientDetailsProvider getOAuth2ClientDetailsProvider() throws PropertyVetoException {
//        return new OAuth2ClientDetailsProviderImpl(clientDetailsRepository);
//    }

//    @Bean
//    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        registration.setFilter(filter);
//        registration.setOrder(-100);
//        return registration;
//    }

//    @Bean
//    public CachedServiceScopeServices getResourceStorage() {
//        return new CachedServiceScopeServices();
//    }

//    @Bean("appTokenServices")
//    public NonRemovingTokenServices getTokenServices() throws PropertyVetoException {
//        NonRemovingTokenServices bean = new NonRemovingTokenServices();
//        bean.setTokenStore(tokenStore);
//        bean.setSupportRefreshToken(true);
//        bean.setReuseRefreshToken(true);
//        bean.setAccessTokenValiditySeconds(accessTokenValidity);
//        bean.setRefreshTokenValiditySeconds(refreshTokenValidity);
//        bean.setClientDetailsService(getClientDetails());
//        if (oauth2UseJwt) {
//            bean.setTokenEnhancer(new AACTokenEnhancer(tokenEnhancer, tokenConverter));
//        } else {
//            bean.setTokenEnhancer(new AACTokenEnhancer(tokenEnhancer));
//        }
//
//        return bean;
//    }

//    /*
//     * Authorities handlers
//     */
//
//    @Bean
//    public IdentitySource getIdentitySource() {
//        return new FileEmailIdentitySource();
//    }
//
//    @Bean
//    public AuthorityHandlerContainer getAuthorityHandlerContainer() {
//        Map<String, AuthorityHandler> map = Maps.newTreeMap();
//        map.put(Config.IDP_INTERNAL, getInternalHandler());
//        FBAuthorityHandler fh = new FBAuthorityHandler();
//        map.put("facebook", fh);
//        AuthorityHandlerContainer bean = new AuthorityHandlerContainer(map);
//        return bean;
//    }
//
//    @Bean
//    public DefaultAuthorityHandler getDefaultHandler() {
//        return new DefaultAuthorityHandler();
//    }
//
//    @Bean
//    public InternalAuthorityHandler getInternalHandler() {
//        return new InternalAuthorityHandler();
//    }

}
