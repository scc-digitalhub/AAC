package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.google.common.collect.Maps;

import it.smartcommunitylab.aac.Config;

@Configuration
public class AACConfig {

    @Value("${oauth2.jwt}")
    private boolean oauth2UseJwt;

    @Value("${oauth2.accesstoken.validity}")
    private int accessTokenValidity;

    @Value("${oauth2.refreshtoken.validity}")
    private int refreshTokenValidity;



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
