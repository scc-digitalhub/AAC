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
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import java.util.HashMap;
import java.util.Map;

public class RefreshTokenGranter extends AbstractTokenGranter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GRANT_TYPE = "refresh_token";

    // optional token store, used to recover the full original authentication
    // (including the end-user) for audit events during the refresh flow
    private ExtTokenStore tokenStore;

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
     * Recover the full original authentication (including the end-user) bound to the
     * refresh token. The base implementation builds a client-only authentication with
     * a null user, which would strip the end-user context from audit events fired for
     * the refresh_token grant. Falls back to the base behaviour if the authentication
     * cannot be recovered.
     */
    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        if (tokenStore != null) {
            try {
                String refreshTokenValue = tokenRequest.getRequestParameters().get("refresh_token");
                if (refreshTokenValue != null) {
                    OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(refreshTokenValue);
                    if (refreshToken != null) {
                        OAuth2Authentication storedAuth = tokenStore.readAuthenticationForRefreshToken(refreshToken);
                        if (storedAuth != null && storedAuth.getUserAuthentication() != null) {

                            OAuth2Request pendingOAuth2Request = storedAuth.getOAuth2Request();

                            // Combine original request parameters with the new ones from the token request.
                            // New parameters will override the old ones in case of clashes.
                            Map<String, String> combinedParameters = new HashMap<>(
                                pendingOAuth2Request.getRequestParameters()
                            );
                            combinedParameters.putAll(tokenRequest.getRequestParameters());

                            // Create a new OAuth2Request instance using the merged parameters.
                            // This updates the request context while keeping the original client and scopes.
                            OAuth2Request finalStoredOAuth2Request = pendingOAuth2Request.createOAuth2Request(combinedParameters);

                            // Retrieve the original user authentication, including the bound accounts.
                            // This ensures the refreshed token retains the exact same user context.
                            Authentication userAuth = storedAuth.getUserAuthentication();

                            return new OAuth2Authentication(finalStoredOAuth2Request, userAuth);
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("unable to recover full authentication for refresh token audit event: " + e.getMessage());
            }
        }

        return super.getOAuth2Authentication(client, tokenRequest);
    }

    public void setTokenStore(ExtTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }
}
