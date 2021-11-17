package it.smartcommunitylab.aac.oauth.approval;

import org.springframework.security.oauth2.provider.AuthorizationRequest;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public interface ClientApprovalHandler {

    /*
     * evaluate if resourceIds are approved for the given client
     */
    boolean isApproved(AuthorizationRequest authorizationRequest, OAuth2ClientDetails clientDetails);

    /*
     * Hook for allowing modifications to the request to modify based on resourceIds
     * approval
     */
    AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest,
            OAuth2ClientDetails clientDetails);

}
