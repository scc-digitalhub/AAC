package it.smartcommunitylab.aac.claims;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.model.User;

public class ScriptClaimsExtractor implements ResourceClaimsExtractor {

    private final String resourceId;
    private final String userClaimsFunction;
    private final String clientClaimsFunction;

    private final ScriptExecutionService executionService;

    public ScriptClaimsExtractor(String resourceId, String userClaimsFunction, String clientClaimsFunction,
            ScriptExecutionService executionService) {
        Assert.hasText(resourceId, "resourceId can not be null or blank");
        Assert.notNull(executionService, "an execution service is needed");
        Assert.isTrue(validateScript(userClaimsFunction), "user script function is invalid");
        Assert.isTrue(validateScript(clientClaimsFunction), "user script function is invalid");

        this.resourceId = resourceId;
        this.userClaimsFunction = userClaimsFunction;
        this.clientClaimsFunction = clientClaimsFunction;
        this.executionService = executionService;
    }

    private boolean validateScript(String scriptCode) {
        // TODO
        return true;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public ClaimsSet extractUserClaims(User user, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        // execute custom mapping function via executor
//        Map<String, Serializable> customClaims = executionService.executeFunction(name, function, input);
        return null;
    }

    @Override
    public ClaimsSet extractClientClaims(ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException {
        // TODO Auto-generated method stub
        return null;
    }

    private ClaimsSet buildClaimsSet(Map<String, Serializable> map) {
        return null;
    }
}
