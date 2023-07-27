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

package it.smartcommunitylab.aac.oauth;

import it.smartcommunitylab.aac.oauth.approval.ScopeApprovalHandler;
import it.smartcommunitylab.aac.oauth.approval.SpacesApprovalHandler;
import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensionsHandler;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.util.Assert;

public class AACApprovalHandler implements UserApprovalHandler {

    private final UserApprovalHandler userApprovalHandler;
    private ScopeApprovalHandler scopeApprovalHandler;
    private SpacesApprovalHandler spacesApprovalHandler;
    private OAuthFlowExtensionsHandler flowExtensionsHandler;

    public AACApprovalHandler(UserApprovalHandler userApprovalHandler) {
        Assert.notNull(userApprovalHandler, "a user approval handler is required");
        this.userApprovalHandler = userApprovalHandler;
    }

    public void setScopeApprovalHandler(ScopeApprovalHandler scopeApprovalHandler) {
        this.scopeApprovalHandler = scopeApprovalHandler;
    }

    public void setSpacesApprovalHandler(SpacesApprovalHandler spacesApprovalHandler) {
        this.spacesApprovalHandler = spacesApprovalHandler;
    }

    public void setFlowExtensionsHandler(OAuthFlowExtensionsHandler flowExtensionsHandler) {
        this.flowExtensionsHandler = flowExtensionsHandler;
    }

    @Override
    public boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
        boolean result = userApprovalHandler.isApproved(authorizationRequest, userAuthentication);

        // check flow extensions
        if (flowExtensionsHandler != null) {
            result = flowExtensionsHandler.isApproved(authorizationRequest, userAuthentication);
        }

        return result;
    }

    @Override
    public AuthorizationRequest checkForPreApproval(
        AuthorizationRequest authorizationRequest,
        Authentication userAuthentication
    ) {
        AuthorizationRequest request = authorizationRequest;
        if (scopeApprovalHandler != null) {
            // first call scopeApprover to let it modify request *before* user approval
            request = scopeApprovalHandler.checkForPreApproval(request, userAuthentication);
        }

        request = userApprovalHandler.checkForPreApproval(request, userAuthentication);

        // check spaces
        if (spacesApprovalHandler != null) {
            request = spacesApprovalHandler.checkForPreApproval(request, userAuthentication);
        }

        return request;
    }

    @Override
    public AuthorizationRequest updateAfterApproval(
        AuthorizationRequest authorizationRequest,
        Authentication userAuthentication
    ) {
        AuthorizationRequest request = userApprovalHandler.updateAfterApproval(
            authorizationRequest,
            userAuthentication
        );

        // update spaces
        if (spacesApprovalHandler != null) {
            request = spacesApprovalHandler.updateAfterApproval(request, userAuthentication);
        }

        if (scopeApprovalHandler != null) {
            // call scopeApprover after user approver to let it "unauthorize" an approved
            // request
            request = scopeApprovalHandler.updateAfterApproval(request, userAuthentication);
        }

        return request;
    }

    @Override
    public Map<String, Object> getUserApprovalRequest(
        AuthorizationRequest authorizationRequest,
        Authentication userAuthentication
    ) {
        Map<String, Object> model = new HashMap<>();

        Map<String, Object> userApprovalModel = userApprovalHandler.getUserApprovalRequest(
            authorizationRequest,
            userAuthentication
        );
        model.putAll(userApprovalModel);

        // get spaces model
        if (spacesApprovalHandler != null) {
            Map<String, Object> spacesApprovalModel = spacesApprovalHandler.getUserApprovalRequest(
                authorizationRequest,
                userAuthentication
            );
            model.putAll(spacesApprovalModel);
        }

        return model;
    }
}
