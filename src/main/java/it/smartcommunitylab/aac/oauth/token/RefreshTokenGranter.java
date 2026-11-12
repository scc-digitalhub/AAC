/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oauth.token;

import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthenticationToken;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;


public class RefreshTokenGranter extends AbstractTokenGranter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GRANT_TYPE = "refresh_token";

    // optional token store, used to recover the full original authentication
    // (including the end-user) for audit events during the refresh flow
    private TokenStore tokenStore;

    public RefreshTokenGranter(
        AuthorizationServerTokenServices tokenServices,
        OAuth2ClientDetailsService clientDetailsService,
        OAuth2RequestFactory requestFactory
    ) {
        this(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    }

    protected RefreshTokenGranter(
        AuthorizationServerTokenServices tokenServices,
        OAuth2ClientDetailsService clientDetailsService,
        OAuth2RequestFactory requestFactory,
        String grantType
    ) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
    }

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest, OAuth2ClientAuthenticationToken clientAuth ) {
        OAuth2AccessToken token = super.grant(grantType, tokenRequest, clientAuth);
        if (token != null) {
            logger.trace(
                "grant access token for client " +
                tokenRequest.getClientId() +
                " request " +
                tokenRequest.getRequestParameters().toString()
            );
        }

        return token;
    }

    @Override
    protected OAuth2AccessToken getAccessToken(
        ClientDetails client,
        TokenRequest tokenRequest,
        OAuth2Authentication authentication
    ) {
        String refreshToken = tokenRequest.getRequestParameters().get("refresh_token");
        logger.trace("get access token for refresh token " + refreshToken);
        return getTokenServices().refreshAccessToken(refreshToken, tokenRequest);
    }

    /*
     * Overrides the base behavior to recover the full original authentication (including the end-user)
     * bound to the refresh token.
     * The default implementation in AbstractTokenGranter returns an OAuth2Authentication with a null user.
     * We intercept this to attach the historical userAuthentication retrieved from the TokenStore,
     * ensuring the refreshed token retains the exact same user context for audit events and token enhancement.
     */
    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        // 1. Get the base client-only authentication from the framework (user is null here)
        OAuth2Authentication oAuth2Authentication = super.getOAuth2Authentication(client, tokenRequest);

        // Early exit if tokenStore is not configured
        if (tokenStore == null) {
            return oAuth2Authentication;
        }

        try {
            String refreshTokenValue = tokenRequest.getRequestParameters().get("refresh_token");
            if (refreshTokenValue != null) {
                OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(refreshTokenValue);
                if (refreshToken != null) {

                    // 2. Retrieve the historical authentication from the store
                    OAuth2Authentication storedAuth = tokenStore.readAuthenticationForRefreshToken(refreshToken);

                    if (storedAuth != null && storedAuth.getUserAuthentication() != null) {
                        // 3. Attach the recovered user authentication to the current request
                        return new OAuth2Authentication(oAuth2Authentication.getOAuth2Request(), storedAuth.getUserAuthentication());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("unable to recover full authentication for refresh token audit event: " + e.getMessage());
        }

        // Fallback to base behavior if user recovery fails
        return oAuth2Authentication;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }
}
