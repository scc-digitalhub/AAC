package it.smartcommunitylab.aac.oauth;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensions;

public class AACApprovalHandler implements UserApprovalHandler {

    private final UserApprovalHandler userApprovalHandler;
    private UserApprovalHandler scopeApprovalHandler;
    private OAuthFlowExtensions flowExtensions;

    public AACApprovalHandler(UserApprovalHandler userApprovalHandler) {
        Assert.notNull(userApprovalHandler, "a user approval handler is required");
        this.userApprovalHandler = userApprovalHandler;
    }

    public void setScopeApprovalHandler(UserApprovalHandler scopeApprovalHandler) {
        this.scopeApprovalHandler = scopeApprovalHandler;
    }

    public void setFlowExtensions(OAuthFlowExtensions flowExtensions) {
        this.flowExtensions = flowExtensions;
    }

    @Override
    public boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
        boolean result = userApprovalHandler.isApproved(authorizationRequest, userAuthentication);
        // TODO add space selection here

        return result;
    }

    @Override
    public AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
        AuthorizationRequest request = userApprovalHandler.checkForPreApproval(authorizationRequest,
                userAuthentication);
        if (scopeApprovalHandler != null) {
            request = scopeApprovalHandler.checkForPreApproval(request, userAuthentication);
        }

        return request;
    }

    @Override
    public AuthorizationRequest updateAfterApproval(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
        AuthorizationRequest request = userApprovalHandler.updateAfterApproval(authorizationRequest,
                userAuthentication);
        if (scopeApprovalHandler != null) {
            request = scopeApprovalHandler.updateAfterApproval(request, userAuthentication);
        }

        if (flowExtensions != null && isApproved(request, userAuthentication)) {
            flowExtensions.onAfterApproval(request, userAuthentication);
        }

        return request;
    }

    @Override
    public Map<String, Object> getUserApprovalRequest(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
        return userApprovalHandler.getUserApprovalRequest(authorizationRequest, userAuthentication);
    }

}
