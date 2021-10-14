package it.smartcommunitylab.aac.oauth.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public class DelegateOAuthFlowExtensions implements OAuthFlowExtensions {

    private List<OAuthFlowExtensions> flowExtensions;

    public DelegateOAuthFlowExtensions(OAuthFlowExtensions... flowExtensions) {
        this(Arrays.asList(flowExtensions));
    }

    public DelegateOAuthFlowExtensions(List<OAuthFlowExtensions> flowExtensions) {
        Assert.notNull(flowExtensions, "extensions can not be null");
        setFlowExtensions(flowExtensions);
    }

    public void setFlowExtensions(List<OAuthFlowExtensions> flowExtensions) {
        this.flowExtensions = new ArrayList<>(flowExtensions);
    }

    @Override
    public Map<String, String> onBeforeUserApproval(Map<String, String> requestParameters, User user,
            OAuth2ClientDetails client) throws FlowExecutionException {
        // iterate and let all extensions process hook
        Map<String, String> parameters = new HashMap<>();
        parameters.putAll(requestParameters);

        for (OAuthFlowExtensions fe : flowExtensions) {
            parameters = fe.onBeforeUserApproval(parameters, user, client);
        }

        return parameters;

    }

    @Override
    public Boolean onAfterUserApproval(Collection<String> scopes, User user, OAuth2ClientDetails client)
            throws FlowExecutionException {
        // iterate and let all extensions process hook
        // null by default, we don't want to modify the decision
        Boolean result = null;

        for (OAuthFlowExtensions fe : flowExtensions) {
            result = fe.onAfterUserApproval(scopes, user, client);
        }

        return result;
    }

    @Override
    public Map<String, String> onBeforeTokenGrant(Map<String, String> requestParameters, OAuth2ClientDetails client)
            throws FlowExecutionException {
        // iterate and let all extensions process hook
        Map<String, String> parameters = new HashMap<>();
        parameters.putAll(requestParameters);

        for (OAuthFlowExtensions fe : flowExtensions) {
            parameters = fe.onBeforeTokenGrant(parameters, client);
        }

        return parameters;
    }

    @Override
    public void onAfterTokenGrant(OAuth2AccessToken accessToken, OAuth2ClientDetails client)
            throws FlowExecutionException {
        // iterate and let all extensions process hook

        for (OAuthFlowExtensions fe : flowExtensions) {
            fe.onAfterTokenGrant(accessToken, client);
        }

    }

}
