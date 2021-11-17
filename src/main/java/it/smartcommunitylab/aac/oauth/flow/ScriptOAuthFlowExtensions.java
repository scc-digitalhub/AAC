package it.smartcommunitylab.aac.oauth.flow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.dto.UserProfile;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public class ScriptOAuthFlowExtensions implements OAuthFlowExtensions {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
    private final TypeReference<HashMap<String, String>> stringMapTypeRef = new TypeReference<HashMap<String, String>>() {
    };

    private ScriptExecutionService executionService;

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public Map<String, String> onBeforeUserApproval(Map<String, String> requestParameters, User user,
            OAuth2ClientDetails client) throws FlowExecutionException {
        if (executionService == null || client.getHookFunctions() == null) {
            return null;
        }

        String functionName = OAuthFlowExtensions.BEFORE_USER_APPROVAL;
        String functionCode = client.getHookFunctions().get(functionName);
        if (!StringUtils.hasText(functionCode)) {
            return null;
        }

        // convert to profile beans
        // TODO client
        UserProfile profile = new UserProfile(user);

        // convert to map
        Map<String, Serializable> map = new HashMap<>();
        map.put("request", mapper.convertValue(requestParameters, stringMapTypeRef));
        map.put("user", mapper.convertValue(profile, serMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));

        // execute script
        try {
            Map<String, Serializable> customParams = executionService.executeFunction(functionName,
                    functionCode, map);

            // convert back
            Map<String, String> result = mapper.convertValue(customParams, stringMapTypeRef);
            return result;

        } catch (SystemException | InvalidDefinitionException e) {
            throw new FlowExecutionException(e.getMessage());
        }
    }

    @Override
    public Boolean onAfterUserApproval(Collection<String> scopes, User user, OAuth2ClientDetails client)
            throws FlowExecutionException {
        if (executionService == null || client.getHookFunctions() == null) {
            return null;
        }

        String functionName = OAuthFlowExtensions.AFTER_USER_APPROVAL;
        String functionCode = client.getHookFunctions().get(functionName);
        if (!StringUtils.hasText(functionCode)) {
            return null;
        }

        // convert to profile beans
        // TODO client
        UserProfile profile = new UserProfile(user);

        // convert to map
        Map<String, Serializable> map = new HashMap<>();
        map.put("scopes", new ArrayList<>(scopes));
        map.put("user", mapper.convertValue(profile, serMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));

        // execute script
        try {
            Map<String, Serializable> customParams = executionService.executeFunction(functionName,
                    functionCode, map);

            // convert back
            ApprovalResult result = mapper.convertValue(customParams, ApprovalResult.class);

            return result.approved;

        } catch (SystemException | InvalidDefinitionException e) {
            throw new FlowExecutionException(e.getMessage());
        }
    }

    @Override
    public Map<String, String> onBeforeTokenGrant(Map<String, String> requestParameters, OAuth2ClientDetails client)
            throws FlowExecutionException {
        if (executionService == null || client.getHookFunctions() == null) {
            return null;
        }

        String functionName = OAuthFlowExtensions.BEFORE_TOKEN_GRANT;
        String functionCode = client.getHookFunctions().get(functionName);
        if (!StringUtils.hasText(functionCode)) {
            return null;
        }

        // convert to map
        Map<String, Serializable> map = new HashMap<>();
        map.put("request", mapper.convertValue(requestParameters, stringMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));

        // execute script
        try {
            Map<String, Serializable> customParams = executionService.executeFunction(functionName,
                    functionCode, map);

            // convert back
            Map<String, String> result = mapper.convertValue(customParams, stringMapTypeRef);
            return result;

        } catch (SystemException | InvalidDefinitionException e) {
            throw new FlowExecutionException(e.getMessage());
        }
    }

    @Override
    public void onAfterTokenGrant(OAuth2AccessToken accessToken, OAuth2ClientDetails client)
            throws FlowExecutionException {
        // not supported

    }

    public class ApprovalResult {
        public Boolean approved;
    }

}
