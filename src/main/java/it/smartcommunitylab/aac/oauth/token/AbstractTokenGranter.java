package it.smartcommunitylab.aac.oauth.token;

import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnsupportedGrantTypeException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import it.smartcommunitylab.aac.oauth.event.OAuth2EventPublisher;
import it.smartcommunitylab.aac.oauth.flow.FlowExtensionsService;
import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensions;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

public abstract class AbstractTokenGranter implements TokenGranter {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final AuthorizationServerTokenServices tokenServices;

    private final OAuth2ClientDetailsService clientDetailsService;

    private final OAuth2RequestFactory requestFactory;

    protected ScopeRegistry scopeRegistry;

    protected FlowExtensionsService flowExtensionsService;

    protected OAuth2EventPublisher eventPublisher = new OAuth2EventPublisher();

    private final String grantType;

    protected AbstractTokenGranter(AuthorizationServerTokenServices tokenServices,
            OAuth2ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory, String grantType) {
        this.clientDetailsService = clientDetailsService;
        this.grantType = grantType;
        this.tokenServices = tokenServices;
        this.requestFactory = requestFactory;
    }

    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {

        if (!this.grantType.equals(grantType)) {
            return null;
        }

        String clientId = tokenRequest.getClientId();
        logger.debug("Getting access token for: " + clientId);

        OAuth2ClientDetails client = clientDetailsService.loadClientByClientId(clientId);

        // check this grant type is available for this client
        validateGrantType(grantType, client);

        // fetch authentication
        OAuth2Authentication authentication = getOAuth2Authentication(client, tokenRequest);

        // validate scopes, checks approval if required
        // DISABLED, we expect the request to be already valid here
//        validateScope(tokenRequest.getScope(), authentication);

        // get token via granter
        OAuth2AccessToken accessToken = getAccessToken(client, tokenRequest, authentication);

        // audit
        if (eventPublisher != null) {
            eventPublisher.publishTokenGrant(accessToken, authentication);
        }

        // check extensions
        if (flowExtensionsService != null) {
            OAuthFlowExtensions ext = flowExtensionsService.getOAuthFlowExtensions(client);
            if (ext != null) {
                // call extension with token
                ext.onAfterTokenGrant(accessToken, client);
            }

        }

        return accessToken;

    }

    protected OAuth2AccessToken getAccessToken(ClientDetails client, TokenRequest tokenRequest,
            OAuth2Authentication authentication) {
        return tokenServices.createAccessToken(authentication);
    }

    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        OAuth2Request storedOAuth2Request = requestFactory.createOAuth2Request(client, tokenRequest);
        return new OAuth2Authentication(storedOAuth2Request, null);
    }

    protected void validateGrantType(String grantType, ClientDetails clientDetails) {
        Collection<String> authorizedGrantTypes = clientDetails.getAuthorizedGrantTypes();
        if (authorizedGrantTypes == null || authorizedGrantTypes.isEmpty()
                || !authorizedGrantTypes.contains(grantType)) {
            throw new UnsupportedGrantTypeException("Unauthorized grant type: " + grantType);
        }
    }

//    protected void validateScope(Set<String> scope, OAuth2Authentication authentication) {
//
//    }

    protected AuthorizationServerTokenServices getTokenServices() {
        return tokenServices;
    }

    protected OAuth2RequestFactory getRequestFactory() {
        return requestFactory;
    }

    public FlowExtensionsService getFlowExtensionsService() {
        return flowExtensionsService;
    }

    public void setFlowExtensionsService(FlowExtensionsService flowExtensionsService) {
        this.flowExtensionsService = flowExtensionsService;
    }

    public void setEventPublisher(OAuth2EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setScopeRegistry(ScopeRegistry scopeRegistry) {
        this.scopeRegistry = scopeRegistry;
    }

}
