package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.SecurityContextAccessor;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.context.WebApplicationContext;
import it.smartcommunitylab.aac.audit.OAuth2EventListener;
import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.core.auth.DefaultSecurityContextAuthenticationHelper;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.ProviderService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.jwt.JWTService;
import it.smartcommunitylab.aac.oauth.AACApprovalHandler;
import it.smartcommunitylab.aac.oauth.OAuth2TokenServices;
import it.smartcommunitylab.aac.oauth.approval.ApprovalStoreUserApprovalHandler;
import it.smartcommunitylab.aac.oauth.approval.ScopeApprovalHandler;
import it.smartcommunitylab.aac.oauth.approval.SpacesApprovalHandler;
import it.smartcommunitylab.aac.oauth.auth.InternalOpaqueTokenIntrospector;
import it.smartcommunitylab.aac.oauth.event.OAuth2EventPublisher;
import it.smartcommunitylab.aac.oauth.flow.FlowExtensionsService;
import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensionsHandler;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntityRepository;
import it.smartcommunitylab.aac.oauth.provider.ClientRegistrationServices;
import it.smartcommunitylab.aac.oauth.request.ExtRedirectResolver;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientRegistrationServices;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import it.smartcommunitylab.aac.oauth.store.InMemoryAuthorizationRequestStore;
import it.smartcommunitylab.aac.oauth.store.AuthorizationRequestStore;
import it.smartcommunitylab.aac.oauth.store.jdbc.AutoJdbcApprovalStore;
import it.smartcommunitylab.aac.oauth.store.jdbc.AutoJdbcAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.store.jdbc.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.oauth.token.AACTokenEnhancer;
import it.smartcommunitylab.aac.oauth.token.AbstractTokenGranter;
import it.smartcommunitylab.aac.oauth.token.AuthorizationCodeTokenGranter;
import it.smartcommunitylab.aac.oauth.token.ClaimsTokenEnhancer;
import it.smartcommunitylab.aac.oauth.token.ClientCredentialsTokenGranter;
import it.smartcommunitylab.aac.oauth.token.DCRTokenEnhancer;
import it.smartcommunitylab.aac.oauth.token.ImplicitTokenGranter;
import it.smartcommunitylab.aac.oauth.token.JwtTokenConverter;
import it.smartcommunitylab.aac.oauth.token.PKCEAwareTokenGranter;
import it.smartcommunitylab.aac.oauth.token.RefreshTokenGranter;
import it.smartcommunitylab.aac.oauth.token.ResourceOwnerPasswordTokenGranter;
import it.smartcommunitylab.aac.openid.service.OIDCTokenServices;
import it.smartcommunitylab.aac.openid.token.IdTokenServices;
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
    public it.smartcommunitylab.aac.oauth.request.OAuth2RequestFactory getOAuth2RequestFactory(
            OAuth2ClientDetailsService oauthClientDetailsService,
            FlowExtensionsService flowExtensionsService,
            ScopeRegistry scopeRegistry)
            throws PropertyVetoException {

        it.smartcommunitylab.aac.oauth.request.OAuth2RequestFactory requestFactory = new it.smartcommunitylab.aac.oauth.request.OAuth2RequestFactory();
        requestFactory.setFlowExtensionsService(flowExtensionsService);
        requestFactory.setScopeRegistry(scopeRegistry);
        requestFactory.setClientDetailsService(oauthClientDetailsService);
        return requestFactory;
    }

    @Bean
    public it.smartcommunitylab.aac.oauth.request.OAuth2RequestValidator getOAuth2RequestValidator(
            RedirectResolver redirectResolver,
            ScopeRegistry scopeRegistry) {
        it.smartcommunitylab.aac.oauth.request.OAuth2RequestValidator requestValidator = new it.smartcommunitylab.aac.oauth.request.OAuth2RequestValidator(
                redirectResolver);
        requestValidator.setScopeRegistry(scopeRegistry);
        return requestValidator;
    }

    @Bean
    public RedirectResolver getRedirectResolver() {
        ExtRedirectResolver redirectResolver = new ExtRedirectResolver(redirectMatchPorts, redirectMatchSubDomains);
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
            OAuth2ClientDetailsService oauthClientDetailsService,
            ScopeRegistry scopeRegistry) {
        ApprovalStoreUserApprovalHandler handler = new ApprovalStoreUserApprovalHandler();
        handler.setApprovalStore(approvalStore);
        handler.setClientDetailsService(oauthClientDetailsService);
        handler.setScopeRegistry(scopeRegistry);
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
        ApprovalStoreUserApprovalHandler userHandler = userApprovalHandler(approvalStore, oauthClientDetailsService, scopeRegistry);
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
    public JwtTokenConverter jwtTokenEnhancer(JWTService jwtService,
            OAuth2ClientDetailsService oauth2ClientDetailsService) {

        JwtTokenConverter converter = new JwtTokenConverter(issuer, jwtService, oauth2ClientDetailsService);
        converter.setUseJwtByDefault(oauth2UseJwt);
        return converter;
    }

    @Bean
    public DCRTokenEnhancer dcrTokenEnhancer() {
        return new DCRTokenEnhancer();
    }

    @Bean
    public AACTokenEnhancer aacTokenEnhancer(ClaimsTokenEnhancer claimsEnhancer,
//            OIDCTokenEnhancer oidcEnhancer, 
            DCRTokenEnhancer dcrTokenEnhancer,
            JwtTokenConverter tokenConverter) {
        AACTokenEnhancer enhancer = new AACTokenEnhancer();
        enhancer.setClaimsEnhancer(claimsEnhancer);
//        enhancer.setOidcEnhancer(oidcEnhancer);
        enhancer.setDcrTokenEnhancer(dcrTokenEnhancer);
        enhancer.setTokenConverter(tokenConverter);

        return enhancer;
    }

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
            UserService userService,
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
        clientCredentialsTokenGranter.setScopeRegistry(scopeRegistry);
        clientCredentialsTokenGranter.setClientService(clientService);
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
            passwordTokenGranter.setScopeRegistry(scopeRegistry);
            passwordTokenGranter.setClientService(clientService);
            passwordTokenGranter.setUserService(userService);
            granters.add(passwordTokenGranter);
        }

        return new CompositeTokenGranter(new ArrayList<>(granters));
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public AuthorizationRequestStore authorizationRequestRepository() {
        // used as session scoped proxy, we need this for in flight requests
        return new InMemoryAuthorizationRequestStore();
    }

    @Bean
    public IdTokenServices idTokenServices(
            OpenIdClaimsExtractorProvider claimsExtractorProvider,
            JWTService jwtService,
            OAuth2ClientDetailsService clientDetailsService,
            it.smartcommunitylab.aac.core.service.ClientDetailsService clientService) {

        OIDCTokenServices idTokenServices = new OIDCTokenServices(issuer, claimsExtractorProvider,
                jwtService);

        idTokenServices.setClientService(clientService);
        idTokenServices.setClientDetailsService(clientDetailsService);

        return idTokenServices;

    }

    @Bean
    public ClientRegistrationServices clientRegistrationServices(
            OAuth2ClientService clientService,
            ProviderService providerService) {
        OAuth2ClientRegistrationServices dcrServices = new OAuth2ClientRegistrationServices(clientService);
        dcrServices.setProviderService(providerService);

        return dcrServices;
    }

    @Bean
    public InternalOpaqueTokenIntrospector tokenIntrospector(ExtTokenStore tokenStore,
            UserEntityService userService, ClientEntityService clientService) {
        InternalOpaqueTokenIntrospector introspector = new InternalOpaqueTokenIntrospector(tokenStore);
        introspector.setUserService(userService);
        introspector.setClientService(clientService);

        return introspector;
    }

    @Bean
    public OAuth2EventListener oauth2EventListener(OAuth2ClientDetailsService clientDetailsService) {
        return new OAuth2EventListener(clientDetailsService);
    }

}
