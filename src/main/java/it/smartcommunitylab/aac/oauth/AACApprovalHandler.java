package it.smartcommunitylab.aac.oauth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensions;
import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensionsHandler;

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
    public AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
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
    public AuthorizationRequest updateAfterApproval(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
        AuthorizationRequest request = userApprovalHandler.updateAfterApproval(authorizationRequest,
                userAuthentication);

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
    public Map<String, Object> getUserApprovalRequest(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
        Map<String, Object> model = new HashMap<>();

        Map<String, Object> userApprovalModel = userApprovalHandler.getUserApprovalRequest(authorizationRequest,
                userAuthentication);
        model.putAll(userApprovalModel);

        // get spaces model
        if (spacesApprovalHandler != null) {
            Map<String, Object> spacesApprovalModel = spacesApprovalHandler.getUserApprovalRequest(authorizationRequest,
                    userAuthentication);
            model.putAll(spacesApprovalModel);
        }

        return model;
    }

}
