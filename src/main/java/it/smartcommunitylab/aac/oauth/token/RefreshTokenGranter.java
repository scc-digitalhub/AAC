package it.smartcommunitylab.aac.oauth.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

public class RefreshTokenGranter extends AbstractTokenGranter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GRANT_TYPE = "refresh_token";

    public RefreshTokenGranter(AuthorizationServerTokenServices tokenServices,
            OAuth2ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory) {
        this(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    }

    protected RefreshTokenGranter(AuthorizationServerTokenServices tokenServices,
            OAuth2ClientDetailsService clientDetailsService,
            OAuth2RequestFactory requestFactory, String grantType) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
    }

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        OAuth2AccessToken token = super.grant(grantType, tokenRequest);
        if (token != null) {
            logger.trace("grant access token for client " + tokenRequest.getClientId() + " request "
                    + tokenRequest.getRequestParameters().toString());
        }

        return token;
    }

    @Override
    protected OAuth2AccessToken getAccessToken(ClientDetails client, TokenRequest tokenRequest,
            OAuth2Authentication authentication) {
        String refreshToken = tokenRequest.getRequestParameters().get("refresh_token");
        logger.trace("get access token for refresh token " + refreshToken);
        return getTokenServices().refreshAccessToken(refreshToken, tokenRequest);
    }

}
