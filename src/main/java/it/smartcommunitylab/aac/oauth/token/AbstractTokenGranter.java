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

public abstract class AbstractTokenGranter implements TokenGranter {

    protected final Log logger = LogFactory.getLog(getClass());

    private final AuthorizationServerTokenServices tokenServices;

    private final ClientDetailsService clientDetailsService;

    private final OAuth2RequestFactory requestFactory;

    private final String grantType;

    protected AbstractTokenGranter(AuthorizationServerTokenServices tokenServices,
            ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory, String grantType) {
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
        ClientDetails client = clientDetailsService.loadClientByClientId(clientId);
        validateGrantType(grantType, client);

        logger.debug("Getting access token for: " + clientId);

        return getAccessToken(client, tokenRequest);

    }

    protected OAuth2AccessToken getAccessToken(ClientDetails client, TokenRequest tokenRequest) {
        return tokenServices.createAccessToken(getOAuth2Authentication(client, tokenRequest));
    }

    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        OAuth2Request storedOAuth2Request = requestFactory.createOAuth2Request(client, tokenRequest);
        return new OAuth2Authentication(storedOAuth2Request, null);
    }

    protected void validateGrantType(String grantType, ClientDetails clientDetails) {
        Collection<String> authorizedGrantTypes = clientDetails.getAuthorizedGrantTypes();
        logger.trace("authorizedGrantTypes "+String.valueOf(authorizedGrantTypes));
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

}
