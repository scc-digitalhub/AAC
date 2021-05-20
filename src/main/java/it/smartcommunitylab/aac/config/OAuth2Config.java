package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.OAuth2RequestValidator;
import org.springframework.security.oauth2.provider.SecurityContextAccessor;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.DefaultUserApprovalHandler;
import org.springframework.security.oauth2.provider.approval.JdbcApprovalStore;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.filter.CompositeFilter;

import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.auth.DefaultSecurityContextAuthenticationHelper;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.core.service.UserTranslatorService;
import it.smartcommunitylab.aac.jwt.JWTService;
import it.smartcommunitylab.aac.oauth.AACApprovalHandler;
import it.smartcommunitylab.aac.oauth.AACTokenEnhancer;
import it.smartcommunitylab.aac.oauth.ApprovalStoreUserApprovalHandler;
import it.smartcommunitylab.aac.oauth.AutoJdbcApprovalStore;
import it.smartcommunitylab.aac.oauth.AutoJdbcAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.oauth.ClaimsTokenEnhancer;
import it.smartcommunitylab.aac.oauth.ExtRedirectResolver;
import it.smartcommunitylab.aac.oauth.ExtTokenStore;
import it.smartcommunitylab.aac.oauth.JwtTokenConverter;
import it.smartcommunitylab.aac.oauth.NonRemovingTokenServices;
import it.smartcommunitylab.aac.oauth.OAuth2TokenServices;
import it.smartcommunitylab.aac.oauth.PeekableAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.ScopeApprovalHandler;
import it.smartcommunitylab.aac.oauth.SpacesApprovalHandler;
import it.smartcommunitylab.aac.oauth.auth.ClientBasicAuthFilter;
import it.smartcommunitylab.aac.oauth.auth.ClientFormAuthTokenEndpointFilter;
import it.smartcommunitylab.aac.oauth.auth.InternalOpaqueTokenIntrospector;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientSecretAuthenticationProvider;
import it.smartcommunitylab.aac.oauth.event.OAuth2EventPublisher;
import it.smartcommunitylab.aac.oauth.flow.FlowExtensionsService;
import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensionsHandler;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientPKCEAuthenticationProvider;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;
import it.smartcommunitylab.aac.oauth.request.AACOAuth2RequestFactory;
import it.smartcommunitylab.aac.oauth.request.AACOAuth2RequestValidator;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.token.AbstractTokenGranter;
import it.smartcommunitylab.aac.oauth.token.AuthorizationCodeTokenGranter;
import it.smartcommunitylab.aac.oauth.token.ClientCredentialsTokenGranter;
import it.smartcommunitylab.aac.oauth.token.ImplicitTokenGranter;
import it.smartcommunitylab.aac.oauth.token.PKCEAwareTokenGranter;
import it.smartcommunitylab.aac.oauth.token.RefreshTokenGranter;
import it.smartcommunitylab.aac.oauth.token.ResourceOwnerPasswordTokenGranter;
import it.smartcommunitylab.aac.openid.service.OIDCTokenEnhancer;
import it.smartcommunitylab.aac.profiles.claims.OpenIdClaimsExtractorProvider;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

/*
 * OAuth2 services configuration
 */
@Configuration
@Order(6)
public class OAuth2Config {

    @Value("${application.url}")
    private String applicationURL;

    @Value("${jwt.issuer}")
    private String issuer;

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

    @Value("${oauth2.pkce.allowRefresh}")
    private boolean oauth2PKCEAllowRefresh;

    @Value("${oauth2.clientCredentials.allowRefresh}")
    private boolean oauth2ClientCredentialsAllowRefresh;

    @Value("${oauth2.resourceOwnerPassword.allowRefresh}")
    private boolean oauth2ResourceOwnerPasswordAllowRefresh;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuthenticationManager authManager;

    @Bean
    public SecurityContextAccessor securityContextAccessor() {
        return new DefaultSecurityContextAuthenticationHelper();
    }

    @Bean
    public AACOAuth2RequestFactory getOAuth2RequestFactory(
            OAuth2ClientDetailsService clientDetailsService,
            UserService userService,
            FlowExtensionsService flowExtensionsService,
            ScopeRegistry scopeRegistry,
            AuthenticationHelper authenticationHelper)
            throws PropertyVetoException {

        AACOAuth2RequestFactory requestFactory = new AACOAuth2RequestFactory(clientDetailsService);
        requestFactory.setFlowExtensionsService(flowExtensionsService);
        requestFactory.setUserService(userService);
        requestFactory.setScopeRegistry(scopeRegistry);
        requestFactory.setAuthenticationHelper(authenticationHelper);
        return requestFactory;
    }

    @Bean
    public OAuth2RequestValidator getOAuth2RequestValidator(ScopeRegistry scopeRegistry) {
        AACOAuth2RequestValidator requestValidator = new AACOAuth2RequestValidator();
        requestValidator.setScopeRegistry(scopeRegistry);
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
    public OAuth2ClientDetailsService getClientDetailsService(ClientEntityService clientService,
            OAuth2ClientEntityRepository clientRepository) throws PropertyVetoException {
        return new OAuth2ClientDetailsService(clientService, clientRepository);
    }

//    // TODO remove class
//    @Bean
//    public OAuth2ClientUserDetailsService getClientUserDetailsService(OAuth2ClientEntityRepository clientRepository) {
//        return new OAuth2ClientUserDetailsService(clientRepository);
//    }

//    @Bean
//    public UserApprovalHandler getUserApprovalHandler() {
////        // TODO replace our handler
//        return new DefaultUserApprovalHandler();
//    }

    @Bean
    public SessionAttributeStore getLocalSessionAttributeStore() {
        // store in httpSession
        return new DefaultSessionAttributeStore();
    }

    @Bean
    public ExtTokenStore getJDBCTokenStore() throws PropertyVetoException {
        return new AutoJdbcTokenStore(dataSource);
    }

    @Bean
    public AutoJdbcApprovalStore getApprovalStore() throws PropertyVetoException {
        return new AutoJdbcApprovalStore(dataSource);
    }

    public ApprovalStoreUserApprovalHandler userApprovalHandler(
            ApprovalStore approvalStore,
            OAuth2ClientDetailsService oauthClientDetailsService) {
        ApprovalStoreUserApprovalHandler handler = new ApprovalStoreUserApprovalHandler();
        handler.setApprovalStore(approvalStore);
        handler.setClientDetailsService(oauthClientDetailsService);
        return handler;
    }

    public ScopeApprovalHandler scopeApprovalHandler(
            ScopeRegistry scopeRegistry,
            it.smartcommunitylab.aac.core.service.ClientDetailsService clientDetailsService,
            UserService userService) {
        ScopeApprovalHandler handler = new ScopeApprovalHandler(scopeRegistry, clientDetailsService);
        handler.setUserService(userService);

        return handler;
    }

    public SpacesApprovalHandler spacesApprovalHandler(
            it.smartcommunitylab.aac.core.service.ClientDetailsService clientDetailsService,
            UserService userService) {
        SpacesApprovalHandler handler = new SpacesApprovalHandler(clientDetailsService, userService);
        return handler;
    }

    @Bean
    public AACApprovalHandler aacApprovalHandler(
            ApprovalStore approvalStore,
            OAuth2ClientDetailsService oauthClientDetailsService,
            ScopeRegistry scopeRegistry,
            it.smartcommunitylab.aac.core.service.ClientDetailsService clientDetailsService,
            UserService userService,
            FlowExtensionsService flowExtensionsService) {
        ApprovalStoreUserApprovalHandler userHandler = userApprovalHandler(approvalStore, oauthClientDetailsService);
        ScopeApprovalHandler scopeHandler = scopeApprovalHandler(scopeRegistry, clientDetailsService,
                userService);
        SpacesApprovalHandler spacesHandler = spacesApprovalHandler(clientDetailsService, userService);
        OAuthFlowExtensionsHandler flowExtensionsHandler = new OAuthFlowExtensionsHandler(flowExtensionsService,
                oauthClientDetailsService);
        flowExtensionsHandler.setUserService(userService);

        AACApprovalHandler handler = new AACApprovalHandler(userHandler);
        handler.setScopeApprovalHandler(scopeHandler);
        handler.setSpacesApprovalHandler(spacesHandler);
        handler.setFlowExtensionsHandler(flowExtensionsHandler);
        return handler;
    }

    @Bean
    public ClaimsTokenEnhancer claimsTokenEnhancer(ClaimsService claimsService,
            it.smartcommunitylab.aac.core.service.ClientDetailsService clientDetailsService) {
        return new ClaimsTokenEnhancer(claimsService, clientDetailsService);
    }

    @Bean
    public OIDCTokenEnhancer oidcTokenEnhancer(JWTService jwtService,
            it.smartcommunitylab.aac.core.service.ClientDetailsService clientDetailsService,
            OAuth2ClientDetailsService oauth2ClientDetailsService,
            OpenIdClaimsExtractorProvider openidClaimsExtractorProvider) {
//            ClaimsService claimsService) {

        return new OIDCTokenEnhancer(issuer, jwtService, clientDetailsService, oauth2ClientDetailsService,
//                claimsService);
                openidClaimsExtractorProvider);
    }

    @Bean
    public JwtTokenConverter jwtTokenEnhancer(JWTService jwtService,
            OAuth2ClientDetailsService oauth2ClientDetailsService) {

        JwtTokenConverter converter = new JwtTokenConverter(issuer, jwtService, oauth2ClientDetailsService);
        converter.setUseJwtByDefault(oauth2UseJwt);
        return converter;
    }

    @Bean
    public AACTokenEnhancer aacTokenEnhancer(ClaimsTokenEnhancer claimsEnhancer,
            OIDCTokenEnhancer oidcEnhancer,
            JwtTokenConverter tokenConverter) {
        AACTokenEnhancer enhancer = new AACTokenEnhancer();
        enhancer.setClaimsEnhancer(claimsEnhancer);
        enhancer.setOidcEnhancer(oidcEnhancer);
        enhancer.setTokenConverter(tokenConverter);

        return enhancer;
    }
//
//    @Bean
//    public AuthorizationServerTokenServices getTokenServices(
//            ClientDetailsService clientDetailsService,
//            TokenStore tokenStore,
//            AACTokenEnhancer tokenEnhancer) throws PropertyVetoException {
//        NonRemovingTokenServices tokenServices = new NonRemovingTokenServices();
//        tokenServices.setAuthenticationManager(authManager);
//        tokenServices.setClientDetailsService(clientDetailsService);
//        tokenServices.setTokenStore(tokenStore);
//        tokenServices.setSupportRefreshToken(true);
//        tokenServices.setReuseRefreshToken(true);
//        tokenServices.setAccessTokenValiditySeconds(accessTokenValidity);
//        tokenServices.setRefreshTokenValiditySeconds(refreshTokenValidity);
//        tokenServices.setTokenEnhancer(tokenEnhancer);
//
//        return tokenServices;
//    }

    @Bean
    public OAuth2TokenServices getTokenServices(
            OAuth2ClientDetailsService clientDetailsService,
            ExtTokenStore tokenStore, ApprovalStore approvalStore,
            AACTokenEnhancer tokenEnhancer) throws PropertyVetoException {
        OAuth2TokenServices tokenServices = new OAuth2TokenServices(tokenStore);
        tokenServices.setClientDetailsService(clientDetailsService);
        tokenServices.setApprovalStore(approvalStore);
        tokenServices.setTokenEnhancer(tokenEnhancer);
        tokenServices.setAccessTokenValiditySeconds(accessTokenValidity);
        tokenServices.setRefreshTokenValiditySeconds(refreshTokenValidity);
        tokenServices.setRemoveExpired(true);
        return tokenServices;
    }

    @Bean
    public TokenGranter getTokenGranter(
            AuthorizationServerTokenServices tokenServices,
            OAuth2ClientDetailsService clientDetailsService,
            AuthorizationCodeServices authorizationCodeServices,
            OAuth2RequestFactory oAuth2RequestFactory,
            it.smartcommunitylab.aac.core.service.ClientDetailsService clientService,
            ScopeRegistry scopeRegistry,
            UserTranslatorService userTranslatorService,
            FlowExtensionsService flowExtensionsService,
            OAuth2EventPublisher oauth2EventPublisher) {

        // build our own list of granters
        List<AbstractTokenGranter> granters = new ArrayList<>();
        // insert PKCE auth code granter as the first one to supersede basic authcode
        PKCEAwareTokenGranter pkceTokenGranter = new PKCEAwareTokenGranter(tokenServices,
                authorizationCodeServices,
                clientDetailsService, oAuth2RequestFactory);
        if (oauth2PKCEAllowRefresh) {
            pkceTokenGranter.setAllowRefresh(true);
        }
        pkceTokenGranter.setFlowExtensionsService(flowExtensionsService);
        pkceTokenGranter.setEventPublisher(oauth2EventPublisher);
        granters.add(pkceTokenGranter);

        // auth code
        AuthorizationCodeTokenGranter authCodeTokenGranter = new AuthorizationCodeTokenGranter(tokenServices,
                authorizationCodeServices, clientDetailsService,
                oAuth2RequestFactory);
        authCodeTokenGranter.setFlowExtensionsService(flowExtensionsService);
        authCodeTokenGranter.setEventPublisher(oauth2EventPublisher);
        granters.add(authCodeTokenGranter);

        // refresh
        RefreshTokenGranter refreshTokenGranter = new RefreshTokenGranter(tokenServices, clientDetailsService,
                oAuth2RequestFactory);
        refreshTokenGranter.setEventPublisher(oauth2EventPublisher);
        granters.add(refreshTokenGranter);

        // implicit
        ImplicitTokenGranter implicitTokenGranter = new ImplicitTokenGranter(tokenServices,
                clientDetailsService, oAuth2RequestFactory);
        implicitTokenGranter.setFlowExtensionsService(flowExtensionsService);
        implicitTokenGranter.setEventPublisher(oauth2EventPublisher);
        granters.add(implicitTokenGranter);

        // client credentials
        ClientCredentialsTokenGranter clientCredentialsTokenGranter = new ClientCredentialsTokenGranter(
                tokenServices,
                clientDetailsService, oAuth2RequestFactory);
        if (oauth2ClientCredentialsAllowRefresh) {
            clientCredentialsTokenGranter.setAllowRefresh(true);
        }
        clientCredentialsTokenGranter.setFlowExtensionsService(flowExtensionsService);
        clientCredentialsTokenGranter.setEventPublisher(oauth2EventPublisher);
        granters.add(clientCredentialsTokenGranter);

        // resource owner password
        if (authManager != null) {
            ResourceOwnerPasswordTokenGranter passwordTokenGranter = new ResourceOwnerPasswordTokenGranter(
                    authManager, tokenServices,
                    clientDetailsService, oAuth2RequestFactory);
            if (!oauth2ResourceOwnerPasswordAllowRefresh) {
                passwordTokenGranter.setAllowRefresh(false);
            }
            passwordTokenGranter.setFlowExtensionsService(flowExtensionsService);
            passwordTokenGranter.setEventPublisher(oauth2EventPublisher);
            granters.add(passwordTokenGranter);
        }

        return new CompositeTokenGranter(new ArrayList<>(granters));
    }

    @Bean
    public AuthorizationEndpoint getAuthorizationEndpoint(
            ClientDetailsService clientDetailsService,
            AuthorizationCodeServices authorizationCodeServices,
            TokenGranter tokenGranter,
            RedirectResolver redirectResolver,
            UserApprovalHandler userApprovalHandler,
            SessionAttributeStore sessionAttributeStore,
            OAuth2RequestFactory oAuth2RequestFactory,
            OAuth2RequestValidator oauth2RequestValidator) {
        AuthorizationEndpoint authEndpoint = new AuthorizationEndpoint();
        authEndpoint.setClientDetailsService(clientDetailsService);
        authEndpoint.setAuthorizationCodeServices(authorizationCodeServices);
        authEndpoint.setTokenGranter(tokenGranter);
        authEndpoint.setRedirectResolver(redirectResolver);
        authEndpoint.setUserApprovalHandler(userApprovalHandler);
        authEndpoint.setSessionAttributeStore(sessionAttributeStore);
        authEndpoint.setOAuth2RequestFactory(oAuth2RequestFactory);
        authEndpoint.setOAuth2RequestValidator(oauth2RequestValidator);

        return authEndpoint;
    }

    @Bean
    public TokenEndpoint getTokenEndpoint(
            ClientDetailsService clientDetailsService,
            TokenGranter tokenGranter,
            RedirectResolver redirectResolver,
            UserApprovalHandler userApprovalHandler,
            SessionAttributeStore sessionAttributeStore,
            OAuth2RequestFactory oAuth2RequestFactory,
            OAuth2RequestValidator oauth2RequestValidator) {
        TokenEndpoint tokenEndpoint = new TokenEndpoint();
        tokenEndpoint.setClientDetailsService(clientDetailsService);
        tokenEndpoint.setTokenGranter(tokenGranter);
        tokenEndpoint.setOAuth2RequestFactory(oAuth2RequestFactory);
        tokenEndpoint.setOAuth2RequestValidator(oauth2RequestValidator);

        return tokenEndpoint;
    }

    @Bean
    public InternalOpaqueTokenIntrospector tokenIntrospector(ExtTokenStore tokenStore,
            UserEntityService userService, ClientEntityService clientService) {
        InternalOpaqueTokenIntrospector introspector = new InternalOpaqueTokenIntrospector(tokenStore);
        introspector.setUserService(userService);
        introspector.setClientService(clientService);

        return introspector;
    }

}
