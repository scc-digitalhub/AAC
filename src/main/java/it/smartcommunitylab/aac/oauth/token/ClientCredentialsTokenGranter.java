package it.smartcommunitylab.aac.oauth.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

public class ClientCredentialsTokenGranter extends AbstractTokenGranter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GRANT_TYPE = "client_credentials";
    private boolean allowRefresh = false;

    public ClientCredentialsTokenGranter(AuthorizationServerTokenServices tokenServices,
            ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory) {
        this(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    }

    protected ClientCredentialsTokenGranter(AuthorizationServerTokenServices tokenServices,
            ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory, String grantType) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
    }

    public void setAllowRefresh(boolean allowRefresh) {
        this.allowRefresh = allowRefresh;
    }

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        logger.trace("grant access token for client " + tokenRequest.getClientId() + " request "
                + tokenRequest.getRequestParameters().toString());

        OAuth2AccessToken token = super.grant(grantType, tokenRequest);
        if (token != null) {
            DefaultOAuth2AccessToken norefresh = new DefaultOAuth2AccessToken(token);
            // The spec says that client credentials should not be allowed to get a refresh
            // token
            if (!allowRefresh) {
                norefresh.setRefreshToken(null);
            }
            token = norefresh;
        }
        return token;
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        OAuth2Authentication clientAuth = super.getOAuth2Authentication(client, tokenRequest);        
        logger.trace("got oauth authentication from request " + clientAuth.toString());

        return clientAuth;
    }
}
