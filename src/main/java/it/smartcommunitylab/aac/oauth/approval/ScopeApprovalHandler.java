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

package it.smartcommunitylab.aac.oauth.approval;

import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import it.smartcommunitylab.aac.users.service.UserService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.util.Assert;

public class ScopeApprovalHandler implements UserApprovalHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ClientDetailsService clientService;
    private final ScopeRegistry scopeRegistry;
    private UserService userService;

    public ScopeApprovalHandler(ScopeRegistry scopeRegistry, ClientDetailsService clientService) {
        Assert.notNull(scopeRegistry, "scope registry is required");
        Assert.notNull(clientService, "client details service is required");
        this.scopeRegistry = scopeRegistry;
        this.clientService = clientService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
        return authorizationRequest.isApproved();
    }

    @Override
    public AuthorizationRequest checkForPreApproval(
        AuthorizationRequest authorizationRequest,
        Authentication userAuth
    ) {
        // we need to do check on preauth because when user has already approved scopes
        // afterAuth won't be invoked.
        try {
            Set<String> scopes = authorizationRequest.getScope();
            ClientDetails clientDetails = clientService.loadClient(authorizationRequest.getClientId());
            UserDetails userDetails = null;

            // check if userAuth is present
            if (userAuth != null && userAuth instanceof UserAuthentication) {
                userDetails = ((UserAuthentication) userAuth).getUser();
            }

            logger.debug("process scopes for client " + clientDetails.getClientId() + ": " + scopes);

            Set<String> approvedScopes = new HashSet<>();

            for (String s : scopes) {
                try {
                    Scope scope = scopeRegistry.getScope(s);
                    ScopeApprover sa = scopeRegistry.getScopeApprover(s);
                    if (sa == null) {
                        // this scope is undecided so skip
                        continue;
                    }

                    Approval approval = null;
                    if (ScopeType.CLIENT == scope.getType()) {
                        approval = sa.approveClientScope(s, clientDetails, scopes);
                    }
                    if (ScopeType.USER == scope.getType() && userDetails != null) {
                        approval =
                            sa.approveUserScope(s, translateUser(userDetails, sa.getRealm()), clientDetails, scopes);
                    }
                    if (ScopeType.GENERIC == scope.getType()) {
                        if (userDetails != null) {
                            approval =
                                sa.approveUserScope(
                                    s,
                                    translateUser(userDetails, sa.getRealm()),
                                    clientDetails,
                                    scopes
                                );
                        } else {
                            approval = sa.approveClientScope(s, clientDetails, scopes);
                        }
                    }

                    if (approval != null) {
                        if (!approval.isApproved()) {
                            // deny the request
                            throw new InvalidClientException("Unauthorized client for scope: " + s);
                        } else {
                            approvedScopes.add(s);
                        }
                    }
                } catch (NoSuchScopeException | SystemException | InvalidDefinitionException e) {
                    // ignore
                }
            }

            logger.debug("approved scopes for client " + clientDetails.getClientId() + ": " + approvedScopes);

            // set on request
            authorizationRequest.setScope(approvedScopes);
            return authorizationRequest;
        } catch (NoSuchClientException e) {
            // block the request
            throw new OAuth2AccessDeniedException();
        }
    }

    @Override
    public AuthorizationRequest updateAfterApproval(
        AuthorizationRequest authorizationRequest,
        Authentication userAuth
    ) {
        return authorizationRequest;
    }

    @Override
    public Map<String, Object> getUserApprovalRequest(
        AuthorizationRequest authorizationRequest,
        Authentication userAuthentication
    ) {
        return null;
    }

    private User translateUser(UserDetails userDetails, String realm) {
        if (userService != null) {
            return userService.getUser(userDetails, realm);
        }

        return new User(userDetails);
    }
    //    protected Set<String> validateScopes(OAuth2Authentication authentication, ClientDetails client,
    //            Set<String> scopes) {
    //
    //        if (clientService == null || scopeRegistry == null || userService == null) {
    //            return scopes;
    //        } else {
    //            try {
    //                // fetch client
    //                it.smartcommunitylab.aac.core.ClientDetails clientDetails = clientService
    //                        .loadClient(client.getClientId());
    //                UserDetails userDetails = null;
    //
    //                // check if userAuth is present
    //                Authentication userAuth = authentication.getUserAuthentication();
    //                if (userAuth != null && userAuth instanceof UserAuthenticationToken) {
    //                    userDetails = ((UserAuthenticationToken) userAuth).getUser();
    //                }
    //
    //                Set<String> approvedScopes = new HashSet<>();
    //
    //                for (String s : scopes) {
    //                    try {
    //                        Scope scope = scopeRegistry.getScope(s);
    //                        ScopeApprover sa = scopeRegistry.getScopeApprover(s);
    //                        if (sa == null) {
    //                            // this scope is undecided so skip
    //                            continue;
    //                        }
    //
    //                        Approval approval = null;
    //                        if (ScopeType.CLIENT == scope.getType()) {
    //                            approval = sa.approveClientScope(s, clientDetails, scopes);
    //                        }
    //                        if (ScopeType.USER == scope.getType() && userDetails != null) {
    //                            approval = sa.approveUserScope(s,
    //                                    userTranslatorService.translate(userDetails, sa.getRealm()), clientDetails,
    //                                    scopes);
    //                        }
    //                        if (ScopeType.GENERIC == scope.getType()) {
    //                            if (userDetails != null) {
    //                                approval = sa.approveUserScope(s,
    //                                        userTranslatorService.translate(userDetails, sa.getRealm()),
    //                                        clientDetails, scopes);
    //                            } else {
    //                                approval = sa.approveClientScope(s, clientDetails, scopes);
    //                            }
    //                        }
    //
    //                        if (approval != null) {
    //                            if (!approval.isApproved()) {
    //                                // deny the request
    //                                throw new InvalidClientException("Unauthorized client for scope: " + s);
    //                            } else {
    //                                approvedScopes.add(s);
    //                            }
    //                        }
    //
    //                    } catch (NoSuchScopeException | SystemException | InvalidDefinitionException e) {
    //                        // ignore
    //                    }
    //
    //                }
    //
    //                return approvedScopes;
    //
    //            } catch (NoSuchClientException e) {
    //                throw new InvalidClientException("Invalid or unauthorized client");
    //            }
    //        }
    //
    //    }

}
