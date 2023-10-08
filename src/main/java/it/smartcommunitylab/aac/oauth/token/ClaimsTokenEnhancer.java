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

import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.users.auth.UserAuthentication;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.util.Assert;

public class ClaimsTokenEnhancer implements TokenEnhancer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ClaimsService claimsService;
    private final ClientDetailsService clientDetailsService;

    public ClaimsTokenEnhancer(ClaimsService claimsService, ClientDetailsService clientDetailsService) {
        Assert.notNull(claimsService, "claims service is mandatory");
        Assert.notNull(clientDetailsService, "client details service is mandatory");
        this.claimsService = claimsService;
        this.clientDetailsService = clientDetailsService;
    }

    @Override
    public AACOAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        logger.debug(
            "enhance access token " +
            accessToken.getTokenType() +
            " for " +
            authentication.getName() +
            " value " +
            accessToken.toString()
        );

        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();
        Set<String> resourceIds = request.getResourceIds();
        Map<String, Serializable> extensions = request.getExtensions();

        try {
            AACOAuth2AccessToken token = new AACOAuth2AccessToken(accessToken);
            Map<String, Serializable> claims = null;

            ClientDetails clientDetails = clientDetailsService.loadClient(clientId);

            // check if client or user
            if (isClientRequest(request)) {
                claims = claimsService.getClientClaims(clientDetails, scopes, resourceIds, extensions);
            } else {
                Authentication userAuth = authentication.getUserAuthentication();
                if (userAuth != null && userAuth instanceof UserAuthentication) {
                    UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
                    // ask claims for the user model appropriate for the client's realm
                    claims =
                        claimsService.getUserClaims(
                            userDetails,
                            clientDetails.getRealm(),
                            clientDetails,
                            scopes,
                            resourceIds,
                            extensions
                        );
                }
            }

            if (claims != null) {
                token.setClaims(claims);
            }

            return token;
        } catch (NoSuchClientException e) {
            logger.error("non existing client: " + e.getMessage());
            throw new InvalidClientException("invalid client");
        } catch (SystemException | NoSuchResourceException | InvalidDefinitionException e) {
            logger.error("claims service error: " + e.getMessage());
            throw new OAuth2Exception(e.getMessage());
        }
    }

    private boolean isClientRequest(OAuth2Request request) {
        return "client_credentials".equals(request.getGrantType());
    }
}
