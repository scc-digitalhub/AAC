package it.smartcommunitylab.aac.oauth.flow;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.core.service.UserTranslatorService;
import it.smartcommunitylab.aac.model.User;

public class OAuthFlowExtensionsHandler implements UserApprovalHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FlowExtensionsService flowExtensionsService;
    private final ClientDetailsService clientService;
    private UserTranslatorService userTranslatorService;

    public OAuthFlowExtensionsHandler(FlowExtensionsService flowExtensionsService, ClientDetailsService clientService) {
        Assert.notNull(flowExtensionsService, "flow extensions service is required");
        Assert.notNull(clientService, "client details service is required");
        this.flowExtensionsService = flowExtensionsService;
        this.clientService = clientService;
    }

    public void setUserTranslatorService(UserTranslatorService userTranslatorService) {
        this.userTranslatorService = userTranslatorService;
    }

    @Override
    public boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuth) {
        try {

            // short circuit for not approved, we don't want to flip
            if (!authorizationRequest.isApproved()) {
                return authorizationRequest.isApproved();
            }

            Set<String> scopes = authorizationRequest.getScope();
            ClientDetails clientDetails = clientService.loadClient(authorizationRequest.getClientId());
            String realm = clientDetails.getRealm();

            UserDetails userDetails = null;
            User user = null;

            // check if userAuth is present
            if (userAuth != null && userAuth instanceof UserAuthenticationToken) {
                userDetails = ((UserAuthenticationToken) userAuth).getUser();
                if (userTranslatorService != null) {
                    user = userTranslatorService.translate(userDetails, realm);
                } else {
                    user = new User(userDetails);
                }

            }

            // get extension if set
            OAuthFlowExtensions ext = flowExtensionsService.getOAuthFlowExtensions(clientDetails);
            if (ext == null) {
                return authorizationRequest.isApproved();
            }

            // call extension and check if we get a valid response
            Boolean approved = ext.onAfterUserApproval(scopes, user, clientDetails);
            if (approved == null) {
                return authorizationRequest.isApproved();
            }

            return approved.booleanValue();

        } catch (NoSuchClientException e) {
            // block the request
            throw new OAuth2AccessDeniedException();
        }

    }

    @Override
    public AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
        // nothing to do here
        return null;
    }

    @Override
    public AuthorizationRequest updateAfterApproval(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
        // nothing to do here
        return authorizationRequest;
    }

    @Override
    public Map<String, Object> getUserApprovalRequest(AuthorizationRequest authorizationRequest,
            Authentication userAuthentication) {
        // nothing to do here
        return null;
    }

}
