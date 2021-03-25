/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.oauth.request;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2RequestValidator;
import org.springframework.security.oauth2.provider.TokenRequest;
import it.smartcommunitylab.aac.Config;

/**
 * Exclude 'operation.confirmed' scope from the client scopes validation
 * 
 * @author raman
 *
 */
public class AACOAuth2RequestValidator implements OAuth2RequestValidator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void validateScope(AuthorizationRequest authorizationRequest, ClientDetails client)
            throws InvalidScopeException {
        validateScope(authorizationRequest.getScope(), client.getScope());
    }

    public void validateScope(TokenRequest tokenRequest, ClientDetails client) throws InvalidScopeException {
        // check grant type and act accordingly
        String grantType = tokenRequest.getGrantType();
        // NOTE that TokenEndpoint will simply ignore requestFactory scopes for refresh
        // grant and insert *after* this check the ones fetched from request WITHOUT
        // VALIDATION!
        if (Config.GRANT_TYPE_PASSWORD.equals(grantType) ||
                Config.GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType) ||
                Config.GRANT_TYPE_REFRESH_TOKEN.equals(grantType)) {
            validateScope(tokenRequest.getScope(), client.getScope());
        } else {
            // enforce spec, no scope associated to these flows
            if (!tokenRequest.getScope().isEmpty()) {
                throw new InvalidScopeException(
                        "token request for " + grantType + " should not have scopes associated");
            }
        }

    }

    private void validateScope(Set<String> requestScopes, Set<String> clientScopes) {

        logger.trace("validate scopes requested " + String.valueOf(requestScopes.toString())
                + " against client " + String.valueOf(clientScopes.toString()));

        Set<String> validScopes = (clientScopes != null ? clientScopes : Collections.emptySet());

        // each scope has to be pre-authorized
        Set<String> unauthorizedScopes = requestScopes.stream().filter(s -> !validScopes.contains(s))
                .collect(Collectors.toSet());

        if (!unauthorizedScopes.isEmpty()) {
            String invalidScopes = String.join(" ", unauthorizedScopes);
            throw new InvalidScopeException("Invalid scope: " + invalidScopes, validScopes);
        }

//        // handle default case
//
//        if (clientScopes != null && !clientScopes.isEmpty()) {
//            for (String scope : requestScopes) {
//                if (Config.SCOPE_OPERATION_CONFIRMED.equals(scope))
//                    continue;
//                if (!clientScopes.contains(scope)) {
//                    throw new InvalidScopeException("Invalid scope: " + scope, clientScopes);
//                }
//            }
//        }

//        if (requestScopes.isEmpty()) {
//            logger.debug("empty scopes");
//            throw new InvalidScopeException(
//                    "Empty scope (either the client or the user is not allowed the requested scopes)");
//        }
    }

}
