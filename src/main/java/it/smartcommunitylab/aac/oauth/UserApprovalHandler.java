/**
 *    Copyright 2012-2013 Trento RISE
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
 */

package it.smartcommunitylab.aac.oauth;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.model.Resource;

import java.util.Set;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.TokenStoreUserApprovalHandler;

/**
 * Extension of {@link TokenStoreUserApprovalHandler} to enable automatic authorization
 * for trusted clients.
 * @author raman
 *
 */
public class UserApprovalHandler extends TokenStoreUserApprovalHandler { // changed

	@Autowired
	private ServletContext servletContext;
	@Autowired 
	private ResourceServices resourceService;
	
	@Override
	public AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
		return super.checkForPreApproval(authorizationRequest, userAuthentication);
	}

	/**
	 * Allows automatic approval for trusted clients.
	 * 
	 * @param authorizationRequest The authorization request.
	 * @param userAuthentication the current user authentication
	 * 
	 * @return Whether the specified request has been approved by the current user.
	 */
	@Override
	public boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {

		// If we are allowed to check existing approvals this will short circuit the decision
		if (super.isApproved(authorizationRequest, userAuthentication)) {
			return true;
		}

		if (!userAuthentication.isAuthenticated()) {
			return false;
		}

		String flag = authorizationRequest.getApprovalParameters().get(OAuth2Utils.USER_OAUTH_APPROVAL); // changed
		boolean approved = flag != null && flag.toLowerCase().equals("true");
		if (approved) return true;
		
		// or trusted client
		if (authorizationRequest.getAuthorities() != null) {
			for (GrantedAuthority ga : authorizationRequest.getAuthorities())
				if (Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString().equals(ga.getAuthority())) return true;
		}
		// or test token redirect uri
		// or accesses only own resources
		return authorizationRequest.getRedirectUri().equals(ExtRedirectResolver.testTokenPath(servletContext))
			   || useOwnResourcesOnly(authorizationRequest.getClientId(), authorizationRequest.getScope());
	}

	/**
	 * @param clientId
	 * @param resourceUris
	 * @return true if the given client requires access only to the resources managed by the client itself
	 */
	private boolean useOwnResourcesOnly(String clientId, Set<String> resourceUris) {
		if (resourceUris != null) {
			for (String uri : resourceUris) {
				Resource r = resourceService.loadResourceByResourceUri(uri);
				if (r == null) {
					continue;
				}
				if (r.getAuthority() == AUTHORITY.ROLE_USER && ! clientId.equals(r.getClientId())) return false;
			}
		}
		return true;
	}


}
