package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.OAuth2RequestValidator;
import org.springframework.security.oauth2.provider.SecurityContextAccessor;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;

import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.auth.DefaultSecurityContextAuthenticationHelper;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.oauth.AACOAuth2RequestValidator;
import it.smartcommunitylab.aac.oauth.AutoJdbcAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.ExtRedirectResolver;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

@Configuration
public class OAuth2Config {

    @Value("${application.url}")
    private String applicationURL;

    @Value("${oauth2.jwt}")
    private boolean oauth2UseJwt;

    @Value("${oauth2.authcode.validity}")
    private int authCodeValidity;

    @Value("${oauth2.accesstoken.validity}")
    private int accessTokenValidity;

    @Value("${oauth2.refreshtoken.validity}")
    private int refreshTokenValidity;

    @Value("${oauth2.redirects.matchports}")
    private boolean redirectMatchPorts;

    @Value("${oauth2.redirects.matchsubdomains}")
    private boolean redirectMatchSubDomains;

    @Autowired
    private DataSource dataSource;

//    @Autowired
//    private ClientEntityService clientService;
    
    @Bean
    public AuthenticationHelper authenticationHelper() {
        return new DefaultSecurityContextAuthenticationHelper();
    }

    @Bean
    public SecurityContextAccessor securityContextAccessor() {
        return new DefaultSecurityContextAuthenticationHelper();
    }

    @Bean
    public OAuth2RequestFactory getOAuth2RequestFactory(
            ClientDetailsService clientDetailsService,
            SecurityContextAccessor securityContextAccessor)
            throws PropertyVetoException {

//      AACOAuth2RequestFactory<UserManager> result = new AACOAuth2RequestFactory<>();
        DefaultOAuth2RequestFactory requestFactory = new DefaultOAuth2RequestFactory(clientDetailsService);
        requestFactory.setCheckUserScopes(false);
        requestFactory.setSecurityContextAccessor(securityContextAccessor);
        return requestFactory;
    }

    @Bean
    public OAuth2RequestValidator getOAuth2RequestValidator() {
        AACOAuth2RequestValidator requestValidator = new AACOAuth2RequestValidator();
        return requestValidator;
    }

    @Bean
    public RedirectResolver getRedirectResolver() {
        ExtRedirectResolver redirectResolver = new ExtRedirectResolver(applicationURL, redirectMatchPorts,
                redirectMatchSubDomains);
        return redirectResolver;
    }

    @Bean
    public AutoJdbcAuthorizationCodeServices getAuthorizationCodeServices() throws PropertyVetoException {
        return new AutoJdbcAuthorizationCodeServices(dataSource, authCodeValidity);
    }

    @Bean
    public ClientDetailsService getClientDetailsService(ClientEntityService clientService,
            OAuth2ClientEntityRepository clientRepository) throws PropertyVetoException {
        return new OAuth2ClientDetailsService(clientService, clientRepository);
    }
}
