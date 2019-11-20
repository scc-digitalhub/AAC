/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.TokenStoreUserApprovalHandler;
import org.springframework.util.StringUtils;

import com.google.common.collect.Multimap;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.model.Resource;

/**
 * Extension of {@link TokenStoreUserApprovalHandler} to enable automatic authorization
 * for trusted clients.
 * @author raman
 *
 */
public class UserApprovalHandler extends TokenStoreUserApprovalHandler { // changed
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ServletContext servletContext;
	@Autowired 
	private ResourceServices resourceService;
	@Autowired
	private RoleManager roleManager;
	
	private static final String SPACE_SELECTION_APPROVAL_REQUIRED = "spaceSelectionApproval_required";
	private static final String SPACE_SELECTION_APPROVAL_DONE = "spaceSelectionApproval_done";
	private static final String SPACE_SELECTION_APPROVAL_MAP = "spaceSelectionApproval_map";
	
	@Override
	public AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
		AuthorizationRequest result = super.checkForPreApproval(authorizationRequest, userAuthentication);
		if (!result.isApproved()) return result;

		// see if the user has to perform the space selection which means 
		Multimap<String, String> spaces = roleManager.getRoleSpacesToNarrow(authorizationRequest.getClientId(), userAuthentication.getAuthorities());
		if (spaces != null && !spaces.isEmpty()) {
			Map<String, String> newParams = new HashMap<String, String>(authorizationRequest.getApprovalParameters());
			authorizationRequest.setApprovalParameters(newParams);
			authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_REQUIRED, "true");
			authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");
		}
		return result;
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

		boolean hasSpacesToSelect = "true".equals(authorizationRequest.getApprovalParameters().get(SPACE_SELECTION_APPROVAL_REQUIRED)) &&
				!"true".equals(authorizationRequest.getApprovalParameters().get(SPACE_SELECTION_APPROVAL_DONE));
		
		// If we are allowed to check existing approvals this will short circuit the decision
		// considering the need to select the role space
		if (super.isApproved(authorizationRequest, userAuthentication) && !hasSpacesToSelect) {
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
				if (Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString().equals(ga.getAuthority()) && !hasSpacesToSelect) return true;
		}
		// or test token redirect uri
		// or accesses only own resources
		return authorizationRequest.getRedirectUri().equals(ExtRedirectResolver.testTokenPath(servletContext))
			   || !hasSpacesToSelect && useOwnResourcesOnly(authorizationRequest.getClientId(), authorizationRequest.getScope());
	}

	
	@Override
	public AuthorizationRequest updateAfterApproval(AuthorizationRequest authorizationRequest,
			Authentication userAuthentication) {
		AuthorizationRequest result = super.updateAfterApproval(authorizationRequest, userAuthentication);
		if (result.getApprovalParameters().containsKey(SPACE_SELECTION_APPROVAL_REQUIRED)) {
			Multimap<String, String> spaces = roleManager.getRoleSpacesToNarrow(authorizationRequest.getClientId(), userAuthentication.getAuthorities());
			if (spaces != null && !spaces.isEmpty()) {
				Map<String, String> newParams = new HashMap<String, String>(authorizationRequest.getApprovalParameters());
				authorizationRequest.setApprovalParameters(newParams);
				
				Map<String, String> selection = new HashMap<>();
				newParams.keySet().forEach(key -> {
					if (key.startsWith(SPACE_SELECTION_APPROVAL_MAP)) {
						selection.put(key.substring(SPACE_SELECTION_APPROVAL_MAP.length() + 1), newParams.get(key));
					}
				});
				
				if (StringUtils.isEmpty(selection.isEmpty())) {
					authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");					
				} else {
					try {
						for (Entry<String, String> entry: selection.entrySet()) {
							if (!spaces.containsKey(entry.getKey()) || !spaces.containsEntry(entry.getKey(), entry.getValue())) {
								authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");					
								break;
							}
							spaces.removeAll(entry.getKey());
						}
						if (spaces.size() > 0) {
							authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");					
						} else {
							authorizationRequest.setAuthorities(roleManager.narrowRoleSpaces(selection, userAuthentication.getAuthorities()));
							authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "true");					
						}
					} catch (Exception e) {
						authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");					
					} 
				}
			}

		}
		return result;
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
