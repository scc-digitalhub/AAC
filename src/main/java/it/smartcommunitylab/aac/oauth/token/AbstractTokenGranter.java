package it.smartcommunitylab.aac.oauth.token;

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
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

public abstract class AbstractTokenGranter implements TokenGranter {

    protected final Log logger = LogFactory.getLog(getClass());

    private final AuthorizationServerTokenServices tokenServices;

    private final OAuth2ClientDetailsService clientDetailsService;

    private final OAuth2RequestFactory requestFactory;

    private FlowExtensionsService flowExtensionsService;

    private OAuth2EventPublisher eventPublisher = new OAuth2EventPublisher();

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
        OAuth2ClientDetails client = clientDetailsService.loadClientByClientId(clientId);
        validateGrantType(grantType, client);
//        tokenRequest.setScope(
//                validateScopes(getOAuth2Authentication(client, tokenRequest), client, tokenRequest.getScope()));

        logger.debug("Getting access token for: " + clientId);
        OAuth2Authentication authentication = getOAuth2Authentication(client, tokenRequest);
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
            throw new InvalidClientException("Unauthorized grant type: " + grantType);
        }
    }

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

}
