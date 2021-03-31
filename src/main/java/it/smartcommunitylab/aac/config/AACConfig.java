package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;
import java.util.Collection;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.google.common.collect.Maps;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.claims.DefaultClaimsService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.claims.LocalGraalExecutionService;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.ExtendedAuthenticationManager;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.auth.DefaultSecurityContextAuthenticationHelper;
import it.smartcommunitylab.aac.core.provider.UserTranslator;
import it.smartcommunitylab.aac.core.service.CoreUserTranslator;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;

/*
 * AAC core config
 */
@Configuration
@Order(1)
public class AACConfig {
    /*
     * Core aac should be bootstrapped at @1
     */

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuthorityManager authorityManager;

    /*
     * provider manager depends on authorities + static config + datasource
     */

    @Autowired
    private ProviderManager providerManager;

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

    /*
     * authManager depends on provider + userService
     */
    @Autowired
    private UserEntityService userService;

    @Bean
    public ExtendedAuthenticationManager extendedAuthenticationManager() throws Exception {
        return new ExtendedAuthenticationManager(providerManager, userService);
    }

    @Bean
    public AuthenticationHelper authenticationHelper() {
        return new DefaultSecurityContextAuthenticationHelper();
    }

    /*
     * we need all beans covering authorities here, otherwise we won't be able to
     * build the authmanager (it depends on providerManager -> authorityManager)
     */
    @Bean
    public OIDCClientRegistrationRepository clientRegistrationRepository() {
        return new OIDCClientRegistrationRepository();
    }

    @Bean
    public SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        return new SamlRelyingPartyRegistrationRepository();
    }

    /*
     * initialize the execution service here and then build claims service
     */

    @Bean
    public LocalGraalExecutionService localGraalExecutionService() {
        return new LocalGraalExecutionService();
    }

    @Bean
    public ClaimsService claimsService(Collection<ScopeClaimsExtractor> scopeExtractors,
            ScriptExecutionService executionService) {
        DefaultClaimsService service = new DefaultClaimsService(scopeExtractors);
        service.setExecutionService(executionService);

        return service;

    }

    /*
     * Cross realm user translator
     */
    @Bean
    public UserTranslator userTranslator() {
        return new CoreUserTranslator();
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
