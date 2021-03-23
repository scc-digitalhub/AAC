package it.smartcommunitylab.aac.oauth.approval;

import it.smartcommunitylab.aac.oauth.RealmAuthorizationRequest;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public interface ClientApprovalHandler {

    /*
     * evaluate if resourceIds are approved for the given client
     */
    boolean isApproved(RealmAuthorizationRequest authorizationRequest, OAuth2ClientDetails clientDetails);

    /*
     * Hook for allowing modifications to the request to modify based on resourceIds
     * approval
     */
    RealmAuthorizationRequest checkForPreApproval(RealmAuthorizationRequest authorizationRequest,
            OAuth2ClientDetails clientDetails);

}
