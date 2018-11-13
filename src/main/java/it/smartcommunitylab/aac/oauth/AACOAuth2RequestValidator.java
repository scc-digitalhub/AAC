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

package it.smartcommunitylab.aac.oauth;

import java.util.Collections;
import java.util.Set;

import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestValidator;

import it.smartcommunitylab.aac.Config;

/**
 * Exclude 'operation.confirmed' scope from the client scopes validation
 * @author raman
 *
 */
public class AACOAuth2RequestValidator extends DefaultOAuth2RequestValidator {

	public void validateScope(AuthorizationRequest authorizationRequest, ClientDetails client) throws InvalidScopeException {
		validateScope(authorizationRequest.getScope(), client.getScope());
	}

	public void validateScope(TokenRequest tokenRequest, ClientDetails client) throws InvalidScopeException {
		validateScope(tokenRequest.getScope(), client.getScope());
	}
	
	private void validateScope(Set<String> requestScopes, Set<String> clientScopes) {

		// handle default case
		if (Collections.singleton("default").equals(requestScopes)) return;
		
		if (clientScopes != null && !clientScopes.isEmpty()) {
			for (String scope : requestScopes) {
				if (Config.SCOPE_OPERATION_CONFIRMED.equals(scope) ||
					Config.OPENID_SCOPE.equals(scope) ||
					"default".equals(scope)) continue;
				if (!clientScopes.contains(scope)) {
					throw new InvalidScopeException("Invalid scope: " + scope, clientScopes);
				}
			}
		}
		
		if (requestScopes.isEmpty()) {
			throw new InvalidScopeException("Empty scope (either the client or the user is not allowed the requested scopes)");
		}
	}

}
