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

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.ApprovalStoreUserApprovalHandler;
import org.springframework.security.oauth2.provider.approval.TokenStoreUserApprovalHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.collect.Multimap;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ServiceScope;

/**
 * Extension of {@link TokenStoreUserApprovalHandler} to enable automatic authorization
 * for trusted clients.
 * @author raman
 *
 */
public class UserApprovalHandler extends ApprovalStoreUserApprovalHandler { // changed
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ServletContext servletContext;
	@Autowired
	private RoleManager roleManager;
    @Autowired
    private UserManager userManager;    
    
	@Autowired
	private ServiceManager serviceManager;	
	
	protected int approvalExpirySeconds = -1;
	protected ApprovalStore approvalStore;
	protected OAuthFlowExtensions flowExtensions;
	
	private static final String SPACE_SELECTION_APPROVAL_REQUIRED = "spaceSelectionApproval_required";
	private static final String SPACE_SELECTION_APPROVAL_DONE = "spaceSelectionApproval_done";
	private static final String SPACE_SELECTION_APPROVAL_MAP = "spaceSelectionApproval_map";


	public void afterPropertiesSet() {
		Assert.state(approvalStore != null, "ApprovalStore must be provided");
	}
	
	@Override
	public AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
	    AuthorizationRequest result = super.checkForPreApproval(authorizationRequest, userAuthentication);
		if (!result.isApproved()) return result;

		// see if the user has to perform the space selection 
		Multimap<String, String> spaces = roleManager.getRoleSpacesToNarrow(authorizationRequest.getClientId(), userAuthentication.getAuthorities());
		if (spaces != null && !spaces.isEmpty()) {
			Map<String, String> newParams = new HashMap<String, String>(authorizationRequest.getApprovalParameters());
			authorizationRequest.setApprovalParameters(newParams);
			authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_REQUIRED, "true");
			authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");
		}
		// result is approved, moving towards completion, doing extensions 
		if (flowExtensions != null && isApproved(authorizationRequest, userAuthentication)) {
			flowExtensions.onAfterApproval(authorizationRequest, userAuthentication);;
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
		if (approved) {
		    return true;
		}
		
		// or trusted client
		if (authorizationRequest.getAuthorities() != null) {
			for (GrantedAuthority ga : authorizationRequest.getAuthorities())
				if (Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString().equals(ga.getAuthority()) && !hasSpacesToSelect) {
				    return true;
				}
		}
		// or test token redirect uri
		if(authorizationRequest.getRedirectUri().equals(ExtRedirectResolver.testTokenPath(servletContext))) {
		    return true;
		}
		
		//or "default" scope only requested
		if(Collections.singleton("default").equals(authorizationRequest.getScope())) {
			return true;
		}
		
		return false;
		// or accesses only own resources
//			   || !hasSpacesToSelect && useOwnResourcesOnly(authorizationRequest.getClientId(), authorizationRequest.getScope());
	}

	
	@Override
	public AuthorizationRequest updateAfterApproval(AuthorizationRequest authorizationRequest,
			Authentication userAuthentication) {
		AuthorizationRequest result = updateScopeApprovals(authorizationRequest, userAuthentication);
		
		if (result.getApprovalParameters().containsKey(SPACE_SELECTION_APPROVAL_REQUIRED)) {
            // note: session with principal contains stale info about roles, fetched at
            // login
            Collection<? extends GrantedAuthority> selectedAuthorities = userAuthentication
                    .getAuthorities();
            // fetch again from db
            try {
                User user = (User) userAuthentication.getPrincipal();
                long userId = Long.parseLong(user.getUsername());
                it.smartcommunitylab.aac.model.User userEntity = userManager.findOne(userId);
                selectedAuthorities = roleManager.buildAuthorities(userEntity);
            } catch (Exception e) {
                // user is not available
                logger.error("user not found: " + e.getMessage());
            }		    
			Multimap<String, String> spaces = roleManager.getRoleSpacesToNarrow(authorizationRequest.getClientId(), selectedAuthorities);
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
							authorizationRequest.setAuthorities(roleManager.narrowRoleSpaces(selection, selectedAuthorities));
							authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "true");					
						}
					} catch (Exception e) {
						authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");					
					} 
				}
			}

		}

		// result is approved, moving towards completion, doing extensions 
		if (flowExtensions != null && isApproved(authorizationRequest, userAuthentication)) {
			flowExtensions.onAfterApproval(authorizationRequest, userAuthentication);;
		}
		return result;
	}

	protected AuthorizationRequest updateScopeApprovals(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
		// Get the approved scopes
		Set<String> requestedScopes = authorizationRequest.getScope();
		Set<String> approvedScopes = new HashSet<String>();
		Set<Approval> approvals = new HashSet<Approval>();

		Date expiry = computeExpiry();

		// Store the scopes that have been approved / denied
		Map<String, String> approvalParameters = authorizationRequest.getApprovalParameters();
		boolean userApproved = !approvalParameters.containsKey(OAuth2Utils.USER_OAUTH_APPROVAL) || approvalParameters.get(OAuth2Utils.USER_OAUTH_APPROVAL).equals(Boolean.TRUE.toString());
		for (String requestedScope : requestedScopes) {
			try {
				ServiceScope s = serviceManager.getServiceScope(requestedScope);
				if (// ask the user only for the resources associated to the user role and not managed by this client
					s.getAuthority().equals(AUTHORITY.ROLE_USER) /*&& !authorizationRequest.getClientId().equals(r.getClientId())*/) {
					approvedScopes.add(requestedScope);
					approvals.add(new Approval(userAuthentication.getName(), authorizationRequest.getClientId(), requestedScope, expiry, userApproved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED));
				}
			} catch (Exception e) {
				logger.error("Error reading resource with uri "+requestedScope+": "+e.getMessage());
			}			
		}
		approvalStore.addApprovals(approvals);

		boolean approved;
//		authorizationRequest.setScope(approvedScopes);
		if (approvedScopes.isEmpty() && !requestedScopes.isEmpty()) {
			approved = false;
		}
		else {
			approved = true;
		}
		authorizationRequest.setApproved(approved);
		return authorizationRequest;
	}
	
	public void setApprovalExpiryInSeconds(int approvalExpirySeconds) {
		this.approvalExpirySeconds = approvalExpirySeconds;
		super.setApprovalExpiryInSeconds(approvalExpirySeconds);
	}
	
	/**
	 * @param store the approval to set
	 */
	public void setApprovalStore(ApprovalStore store) {
		this.approvalStore = store;
		super.setApprovalStore(store);
	}

	public void setFlowExtensions(OAuthFlowExtensions extensions) {
		this.flowExtensions = extensions;
	}
	
	private Date computeExpiry() {
		Calendar expiresAt = Calendar.getInstance();
		if (approvalExpirySeconds == -1) { // use default of 6 months
			expiresAt.add(Calendar.MONTH, 6);
		}
		else {
			expiresAt.add(Calendar.SECOND, approvalExpirySeconds);
		}
		return expiresAt.getTime();
	}

	
//	/**
//	 * @param clientId
//	 * @param resourceUris
//	 * @return true if the given client requires access only to the resources managed by the client itself
//	 */
//	private boolean useOwnResourcesOnly(String clientId, Set<String> resourceUris) {
//		if (resourceUris != null) {
//			for (String uri : resourceUris) {
//				ServiceScope s = scopeRepository.findOne(uri);
//				if (s == null || !s.getAuthority().equals(AUTHORITY.ROLE_USER)) {
//					continue;
//				}
//				
//			}
//		}
//		return true;
//	}
}
