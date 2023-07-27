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

import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.implicit.ImplicitTokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.util.Assert;

public class ImplicitTokenGranter extends AbstractTokenGranter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GRANT_TYPE = "implicit";

    public ImplicitTokenGranter(
        AuthorizationServerTokenServices tokenServices,
        OAuth2ClientDetailsService clientDetailsService,
        OAuth2RequestFactory requestFactory
    ) {
        this(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    }

    protected ImplicitTokenGranter(
        AuthorizationServerTokenServices tokenServices,
        OAuth2ClientDetailsService clientDetailsService,
        OAuth2RequestFactory requestFactory,
        String grantType
    ) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
    }

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        OAuth2AccessToken token = super.grant(grantType, tokenRequest);
        if (token != null) {
            logger.trace(
                "grant access token for client " +
                tokenRequest.getClientId() +
                " request " +
                tokenRequest.getRequestParameters().toString()
            );
            if (token.getRefreshToken() != null) {
                AACOAuth2AccessToken norefresh = new AACOAuth2AccessToken(token);
                // The spec says that implicit should not be allowed to get a refresh
                // token
                norefresh.setRefreshToken(null);
                token = norefresh;
                // TODO we should also remove the refresh token from DB
            }
        }

        return token;
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest clientToken) {
        Authentication userAuth = SecurityContextHolder.getContext().getAuthentication();
        if (userAuth == null || !userAuth.isAuthenticated()) {
            throw new InsufficientAuthenticationException("There is no currently logged in user");
        }
        Assert.state(
            clientToken instanceof ImplicitTokenRequest,
            "An ImplicitTokenRequest is required here. Caller needs to wrap the TokenRequest."
        );

        OAuth2Request requestForStorage = ((ImplicitTokenRequest) clientToken).getOAuth2Request();

        logger.trace("got oauth authentication from security context " + userAuth.toString());
        return new OAuth2Authentication(requestForStorage, userAuth);
    }
}
