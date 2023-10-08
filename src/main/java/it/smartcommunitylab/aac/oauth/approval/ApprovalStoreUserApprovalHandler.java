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

package it.smartcommunitylab.aac.oauth.approval;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.model.PromptMode;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.openid.scope.OfflineAccessScope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import it.smartcommunitylab.aac.users.auth.UserAuthentication;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.TokenStoreUserApprovalHandler;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Extension of {@link TokenStoreUserApprovalHandler} to enable automatic
 * authorization for trusted clients.
 *
 * TODO rewrite to leverage complete oauthAuthentication(user + client details)
 * when available
 *
 * TODO rewrite to better handle flow extensions
 *
 * TODO readd space selection
 *
 * @author raman
 *
 */
public class ApprovalStoreUserApprovalHandler implements UserApprovalHandler, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SCOPE_PREFIX = "";

    private static final int DEFAULT_APPROVAL_VALIDITY = 60 * 60 * 24 * 30; // 30 days

    private int approvalExpirySeconds = DEFAULT_APPROVAL_VALIDITY;

    private ApprovalStore approvalStore;
    //    private OAuthFlowExtensions flowExtensions;
    private OAuth2ClientDetailsService oauthClientDetailsService;
    private ScopeRegistry scopeRegistry;

    //    private it.smartcommunitylab.aac.core.service.ClientDetailsService clientDetailsService;
    //    private UserService userService;

    public void afterPropertiesSet() {
        Assert.notNull(approvalStore, "approval store is required");
        Assert.notNull(oauthClientDetailsService, "oauth client details service is required");
    }

    @Override
    public AuthorizationRequest checkForPreApproval(AuthorizationRequest request, Authentication userAuth) {
        if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
            throw new InvalidRequestException("approval requires a valid user authentication");
        }

        // fetch details
        String clientId = request.getClientId();
        Set<String> requestedScopes = request.getScope();

        // short circuit for no scopes
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            // nothing to ask, approve
            request.setApproved(true);
        }

        UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
        String subjectId = userDetails.getUserId();

        OAuth2ClientDetails clientDetails;
        try {
            clientDetails = oauthClientDetailsService.loadClientByClientId(clientId);
        } catch (ClientRegistrationException e) {
            // non existing client, drop
            throw new InvalidRequestException("invalid client");
        }

        logger.debug("requested scopes for client " + clientId + ": " + String.valueOf(requestedScopes));

        // we have 2 sets of approved scopes
        // firstParty handling: autoApprove for same realm user with autoApprove scopes
        // TODO handle mixed user scopes + client scopes
        Set<String> autoApprovedScopes = getAutoApproved(requestedScopes, userDetails, clientDetails);
        Set<String> userApprovedScopes = getUserApproved(userDetails, clientDetails);

        // persist autoApproved so we'll be able to check for refreshToken
        // we would do this in updateAfterApproval but if all scopes are autoapproved
        // that won't be invoked, we short circuit
        if (autoApprovedScopes.containsAll(requestedScopes)) {
            Date expiresAt = computeExpiry();
            Set<Approval> approvals = new HashSet<>();
            for (String scope : autoApprovedScopes) {
                Approval approval = new Approval(subjectId, clientId, scope, expiresAt, ApprovalStatus.APPROVED);
                approvals.add(approval);
            }
            // persist
            approvalStore.addApprovals(approvals);
        }

        logger.debug("autoApproved scopes for client " + clientId + ": " + autoApprovedScopes.toString());
        logger.debug("userApproved  scopes for client " + clientId + ": " + userApprovedScopes.toString());

        // check if all requested scopes are already approved
        Set<String> approvedScopes = new HashSet<>();
        approvedScopes.addAll(autoApprovedScopes);
        approvedScopes.addAll(userApprovedScopes);

        if (requestedScopes != null && approvedScopes.containsAll(requestedScopes)) {
            // no need to ask user consent, we can approve here
            request.setApproved(true);
        }

        //        Multimap<String, String> spaces = roleManager.getRoleSpacesToNarrow(authorizationRequest.getClientId(),
        //                selectedAuthorities);
        //        if (spaces != null && !spaces.isEmpty()) {
        //            Map<String, String> newParams = new HashMap<String, String>(authorizationRequest.getApprovalParameters());
        //            authorizationRequest.setApprovalParameters(newParams);
        //            authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_REQUIRED, "true");
        //            authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");
        //        }

        // result is approved, moving towards completion, doing extensions
        // TODO rework extension flows
        //        if (flowExtensions != null && isApproved(request, userAuth)) {
        //            flowExtensions.onAfterApproval(request, userAuth);
        //        }
        return request;
    }

    /**
     * Allows automatic approval for trusted clients.
     *
     * @param authorizationRequest The authorization request.
     * @param userAuthentication   the current user authentication
     *
     * @return Whether the specified request has been approved by the current user.
     */
    @Override
    public boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
        // we have already decided at preApproval
        // TODO add space selection here
        return authorizationRequest.isApproved();
        //        boolean hasSpacesToSelect = "true"
        //                .equals(authorizationRequest.getApprovalParameters().get(SPACE_SELECTION_APPROVAL_REQUIRED)) &&
        //                !"true".equals(authorizationRequest.getApprovalParameters().get(SPACE_SELECTION_APPROVAL_DONE));
        //
        //        // If we are allowed to check existing approvals this will short circuit the
        //        // decision
        //        // considering the need to select the role space
        //        if (super.isApproved(authorizationRequest, userAuthentication) && !hasSpacesToSelect) {
        //            return true;
        //        }
        //
        //        if (!userAuthentication.isAuthenticated()) {
        //            return false;
        //        }
        //
        //        String flag = authorizationRequest.getApprovalParameters().get(OAuth2Utils.USER_OAUTH_APPROVAL); // changed
        //        boolean approved = flag != null && flag.toLowerCase().equals("true");
        //        if (approved) {
        //            return true;
        //        }
        //
        //        // or trusted client
        //        if (authorizationRequest.getAuthorities() != null) {
        //            for (GrantedAuthority ga : authorizationRequest.getAuthorities())
        //                if (Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString().equals(ga.getAuthority()) && !hasSpacesToSelect) {
        //                    return true;
        //                }
        //        }
        ////		// or test token redirect uri
        //////		if(authorizationRequest.getRedirectUri().equals(ExtRedirectResolver.testTokenPath(servletContext))) {
        ////		if(ExtRedirectResolver.isLocalRedirect(authorizationRequest.getRedirectUri(), applicationURL)) {
        ////		    return true;
        ////		}
        //
        //        // or "default" scope only requested
        //        if (Collections.singleton("default").equals(authorizationRequest.getScope())) {
        //            return true;
        //        }
        //
        //        return false;
        //        // or accesses only own resources
        ////			   || !hasSpacesToSelect && useOwnResourcesOnly(authorizationRequest.getClientId(), authorizationRequest.getScope());
    }

    @Override
    public AuthorizationRequest updateAfterApproval(AuthorizationRequest request, Authentication userAuth) {
        AuthorizationRequest result = updateScopeApprovals(request, userAuth);
        //        String clientId = request.getClientId();
        //        UserDetails userDetails = ((UserAuthenticationToken) userAuth).getUser();

        // TODO space selection
        //        if (result.getApprovalParameters().containsKey(SPACE_SELECTION_APPROVAL_REQUIRED)) {
        //            // note: session with principal contains stale info about roles, fetched at
        //            // login
        //            Collection<? extends GrantedAuthority> selectedAuthorities = userAuthentication
        //                    .getAuthorities();
        //            // fetch again from db
        //            try {
        //                User user = (User) userAuthentication.getPrincipal();
        //                long userId = Long.parseLong(user.getUsername());
        //                it.smartcommunitylab.aac.model.User userEntity = userManager.findOne(userId);
        //                selectedAuthorities = roleManager.buildAuthorities(userEntity);
        //            } catch (Exception e) {
        //                // user is not available
        //                logger.error("user not found: " + e.getMessage());
        //            }
        //            Multimap<String, String> spaces = roleManager.getRoleSpacesToNarrow(authorizationRequest.getClientId(),
        //                    selectedAuthorities);
        //            if (spaces != null && !spaces.isEmpty()) {
        //                Map<String, String> newParams = new HashMap<String, String>(
        //                        authorizationRequest.getApprovalParameters());
        //                authorizationRequest.setApprovalParameters(newParams);
        //
        //                Map<String, String> selection = new HashMap<>();
        //                newParams.keySet().forEach(key -> {
        //                    if (key.startsWith(SPACE_SELECTION_APPROVAL_MAP)) {
        //                        selection.put(key.substring(SPACE_SELECTION_APPROVAL_MAP.length() + 1), newParams.get(key));
        //                    }
        //                });
        //
        //                if (StringUtils.isEmpty(selection.isEmpty())) {
        //                    authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");
        //                } else {
        //                    try {
        //                        for (Entry<String, String> entry : selection.entrySet()) {
        //                            if (!spaces.containsKey(entry.getKey())
        //                                    || !spaces.containsEntry(entry.getKey(), entry.getValue())) {
        //                                authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE,
        //                                        "false");
        //                                break;
        //                            }
        //                            spaces.removeAll(entry.getKey());
        //                        }
        //                        if (spaces.size() > 0) {
        //                            authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");
        //                        } else {
        //                            authorizationRequest
        //                                    .setAuthorities(roleManager.narrowRoleSpaces(selection, selectedAuthorities));
        //                            authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "true");
        //                        }
        //                    } catch (Exception e) {
        //                        authorizationRequest.getApprovalParameters().put(SPACE_SELECTION_APPROVAL_DONE, "false");
        //                    }
        //                }
        //            }
        //
        //        }

        //        // result is approved, moving towards completion, doing extensions
        //        // TODO rework flow extensions, these should really not be here but after token
        //        // granters
        //        if (flowExtensions != null && isApproved(request, userAuth)) {
        //            flowExtensions.onAfterApproval(request, userAuth);
        //        }

        return result;
    }

    @Override
    public Map<String, Object> getUserApprovalRequest(AuthorizationRequest request, Authentication userAuth) {
        if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
            throw new InvalidRequestException("approval requires a valid user authentication");
        }

        // fetch details
        String clientId = request.getClientId();
        Set<String> requestedScopes = request.getScope();

        UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
        OAuth2ClientDetails clientDetails;
        try {
            clientDetails = oauthClientDetailsService.loadClientByClientId(clientId);
        } catch (ClientRegistrationException e) {
            // non existing client, drop
            throw new InvalidRequestException("invalid client");
        }

        // build the list of scopes requiring user approval
        // we have 2 sets of pre-approved scopes
        Set<String> autoApprovedScopes = getAutoApproved(requestedScopes, userDetails, clientDetails);
        Set<String> userApprovedScopes = getUserApproved(userDetails, clientDetails);

        // check if all requested scopes are already approved
        Set<String> allApprovedScopes = new HashSet<>();
        allApprovedScopes.addAll(autoApprovedScopes);
        allApprovedScopes.addAll(userApprovedScopes);

        // build view model
        Map<String, Object> model = new HashMap<String, Object>();
        model.putAll(request.getRequestParameters());

        //        Map<String, String> scopes = new LinkedHashMap<String, String>();
        //        for (String scope : requestedScopes) {
        //            // set all scopes according to status
        //            if (approvedScopes.contains(scope)) {
        //                scopes.put(SCOPE_PREFIX + scope, "true");
        //            } else {
        //                scopes.put(SCOPE_PREFIX + scope, "false");
        //            }
        //
        //        }
        Set<String> approvalScopes = new HashSet<>();
        Set<String> approvedScopes = new HashSet<>();

        for (String scope : requestedScopes) {
            // set all scopes according to status
            // note offline_access can't be autoapproved or fetched from db...
            if (allApprovedScopes.contains(scope) && !OfflineAccessScope.SCOPE.equals(scope)) {
                approvedScopes.add(scope);
            } else {
                approvalScopes.add(scope);
            }
        }

        // check if prompt consent and clear approved
        Set<String> prompt = StringUtils.commaDelimitedListToSet((String) request.getExtensions().get("prompt"));
        if (prompt.contains(PromptMode.CONSENT.getValue())) {
            approvedScopes = Collections.emptySet();
        }

        // set as oauth2 scope params
        model.put("approval.scope", StringUtils.collectionToDelimitedString(approvedScopes, " "));
        model.put("approved.scope", StringUtils.collectionToDelimitedString(approvedScopes, " "));

        // ensure model contains all scopes from request
        if (!model.containsKey("scope")) {
            model.put("scope", StringUtils.collectionToDelimitedString(requestedScopes, " "));
        }

        // the returned model will be accessible via sessionAttributes as model,
        // or via requestAttributes for additional properties

        return model;
    }

    private AuthorizationRequest updateScopeApprovals(
        AuthorizationRequest authorizationRequest,
        Authentication userAuth
    ) {
        if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
            throw new InvalidRequestException("approval requires a valid user authentication");
        }

        UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
        String subjectId = userDetails.getUserId();

        // Get the approved scopes
        Set<String> requestedScopes = authorizationRequest.getScope();
        Set<String> approvedScopes = new HashSet<String>();
        Set<Approval> approvals = new HashSet<Approval>();

        Date expiry = computeExpiry();

        // Store the scopes that have been approved / denied
        Map<String, String> approvalParameters = authorizationRequest.getApprovalParameters();
        // we support only a global approved flag
        // TODO add per-scope approval to interface and then handle here
        boolean userApproved =
            !approvalParameters.containsKey(OAuth2Utils.USER_OAUTH_APPROVAL) ||
            approvalParameters.get(OAuth2Utils.USER_OAUTH_APPROVAL).equals(Boolean.TRUE.toString());

        //        for (String requestedScope : requestedScopes) {
        //            try {
        //                ServiceScope s = serviceManager.getServiceScope(requestedScope);
        //                if (// ask the user only for the resources associated to the user role and not
        //                    // managed by this client
        //                s.getAuthority().equals(AUTHORITY.ROLE_USER) /*
        //                                                              * &&
        //                                                              * !authorizationRequest.getClientId().equals(r.getClientId
        //                                                              * ())
        //                                                              */) {
        //                    approvedScopes.add(requestedScope);
        //                    approvals.add(new Approval(userAuthentication.getName(), authorizationRequest.getClientId(),
        //                            requestedScope, expiry, userApproved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED));
        //                }
        //            } catch (Exception e) {
        //                logger.error("Error reading resource with uri " + requestedScope + ": " + e.getMessage());
        //            }
        //        }

        for (String requestedScope : requestedScopes) {
            approvals.add(
                new Approval(
                    subjectId,
                    authorizationRequest.getClientId(),
                    requestedScope,
                    expiry,
                    userApproved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED
                )
            );

            if (userApproved) {
                approvedScopes.add(requestedScope);
            }
        }

        approvalStore.addApprovals(approvals);

        // narrow down scopes to those approved
        authorizationRequest.setScope(approvedScopes);

        // check if request can proceed
        if (approvedScopes.isEmpty() && !requestedScopes.isEmpty()) {
            authorizationRequest.setApproved(false);
        } else {
            authorizationRequest.setApproved(true);
        }

        return authorizationRequest;
    }

    private Set<String> getAutoApproved(
        Set<String> requestedScopes,
        UserDetails userDetails,
        OAuth2ClientDetails clientDetails
    ) {
        Set<String> autoApprovedScopes = new HashSet<>();
        if (scopeRegistry != null) {
            // build autoapproved only if client is firstParty same realm
            // TODO evaluate checking request realm
            if (clientDetails.isFirstParty() && userDetails.getRealm().equals(clientDetails.getRealm())) {
                // get from registry core resources
                List<String> coreScopes = scopeRegistry
                    .listResources()
                    .stream()
                    .filter(r -> r.getResourceId().startsWith("aac."))
                    .flatMap(r -> r.getScopes().stream())
                    .map(s -> s.getScope())
                    .collect(Collectors.toList());

                // get same realm resources
                List<String> resourceScopes = scopeRegistry
                    .listResources()
                    .stream()
                    .filter(r -> clientDetails.getRealm().equals(r.getRealm()))
                    .flatMap(r -> r.getScopes().stream())
                    .map(s -> s.getScope())
                    .collect(Collectors.toList());

                autoApprovedScopes =
                    requestedScopes
                        .stream()
                        .filter(s -> (coreScopes.contains(s) || resourceScopes.contains(s)))
                        .collect(Collectors.toSet());
                // get client realm resources
                // TODO, scopes are not per realm for now
            }
        }
        return autoApprovedScopes;
    }

    private Set<String> getUserApproved(UserDetails userDetails, OAuth2ClientDetails clientDetails) {
        Set<String> userApprovedScopes = new HashSet<>();

        // fetch previously approved from store
        Collection<Approval> userApprovals = approvalStore.getApprovals(
            userDetails.getUserId(),
            clientDetails.getClientId()
        );
        Set<Approval> expiredApprovals = new HashSet<>();

        // add those not expired to list and remove others
        for (Approval approval : userApprovals) {
            if (approval.isCurrentlyActive()) {
                // check if approved or denied, we'll let user decide again on denied
                if (approval.getStatus().equals(ApprovalStatus.APPROVED)) {
                    userApprovedScopes.add(approval.getScope());
                }
            } else {
                // inactive means expired, cleanup
                expiredApprovals.add(approval);
            }
        }

        // cleanup expired
        if (!expiredApprovals.isEmpty()) {
            approvalStore.revokeApprovals(expiredApprovals);
        }
        return userApprovedScopes;
    }

    //
    //    private Set<String> getUniqueSpaces(UserDetails userDetails, String uniqueSpaces) {
    //        it.smartcommunitylab.aac.model.User user = userService.getUser(userDetails);
    //        Set<SpaceRole> roles = user.getRoles();
    //
    //        // filter and flatmap everything under context
    //        Set<String> spaces = roles.stream()
    //                .filter(r -> (r.getContext() != null && r.getContext().startsWith(uniqueSpaces))).map(r -> r.getSpace())
    //                .collect(Collectors.toSet());
    //
    //        return spaces;
    //
    //    }

    /*
     * Configuration
     */

    public void setApprovalExpiryInSeconds(int approvalExpirySeconds) {
        this.approvalExpirySeconds = approvalExpirySeconds;
    }

    public void setApprovalStore(ApprovalStore store) {
        this.approvalStore = store;
    }

    public void setClientDetailsService(OAuth2ClientDetailsService oauthClientDetailsService) {
        this.oauthClientDetailsService = oauthClientDetailsService;
    }

    //    public void setFlowExtensions(OAuthFlowExtensions extensions) {
    //        this.flowExtensions = extensions;
    //    }

    //    public void setClientDetailsService(
    //            it.smartcommunitylab.aac.core.service.ClientDetailsService clientDetailsService) {
    //        this.clientDetailsService = clientDetailsService;
    //    }
    //
    //    public void setUserService(UserService userService) {
    //        this.userService = userService;
    //    }

    public void setScopeRegistry(ScopeRegistry scopeRegistry) {
        this.scopeRegistry = scopeRegistry;
    }

    private Date computeExpiry() {
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.SECOND, approvalExpirySeconds);
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
