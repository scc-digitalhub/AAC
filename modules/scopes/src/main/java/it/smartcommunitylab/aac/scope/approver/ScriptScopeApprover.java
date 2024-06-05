package it.smartcommunitylab.aac.scope.approver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.claims.service.ScriptExecutionService;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.dto.UserProfile;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractScopeApprover;
import it.smartcommunitylab.aac.scope.model.ApprovalStatus;
import it.smartcommunitylab.aac.scope.model.Scope;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class ScriptScopeApprover<S extends Scope> extends AbstractScopeApprover<S, LimitedApiScopeApproval> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String APPROVAL_FUNCTION = "approver";
    public static final int DEFAULT_DURATION_S = 3600; // 1h

    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<
        HashMap<String, Serializable>
    >() {};

    private int duration;

    private String functionName;
    private String functionCode;
    private ScriptExecutionService executionService;

    public ScriptScopeApprover(S scope) {
        super(scope);
        this.duration = DEFAULT_DURATION_S;
        this.functionName = APPROVAL_FUNCTION;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public LimitedApiScopeApproval approve(User user, ClientDetails client, Collection<String> scopes) {
        logger.debug(
            "approve user {} for client {} with scopes {}",
            String.valueOf(user.getSubjectId()),
            String.valueOf(client.getClientId()),
            String.valueOf(scopes)
        );

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        if (executionService == null) {
            logger.trace("execution service is null");
            return null;
        }
        if (!StringUtils.hasText(functionCode)) {
            logger.trace("function code is null");
            return null;
        }

        try {
            // convert to profile beans
            // TODO use Context for user and client when implemented
            UserProfile profile = new UserProfile(user);

            // translate user, client and scopes to a map
            Map<String, Serializable> map = new HashMap<>();
            map.put("scopes", new ArrayList<>(scopes));
            map.put("user", mapper.convertValue(profile, serMapTypeRef));
            map.put("client", mapper.convertValue(client, serMapTypeRef));

            // execute script
            Map<String, Serializable> customClaims = executionService.executeFunction(functionName, functionCode, map);

            // we expect result to be compatible with approval
            @SuppressWarnings("unchecked")
            ApprovalResult result = mapper.convertValue(customClaims, ApprovalResult.class);
            if (result.approved == null) {
                return null;
            }

            int expiresIn = duration;
            if (result.expiresAt != null) {
                Date now = new Date();
                expiresIn = (int) Math.abs(result.expiresAt.getTime() - now.getTime());
            }

            ApprovalStatus approvalStatus = result.approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

            logger.debug(
                "approve user {} for client {} with scopes {}: {}",
                user.getSubjectId(),
                client.getClientId(),
                String.valueOf(scopes),
                approvalStatus
            );

            return new LimitedApiScopeApproval(
                scope.getResourceId(),
                scope.getScope(),
                user.getSubjectId(),
                client.getClientId(),
                expiresIn,
                approvalStatus
            );
        } catch (Exception e) {
            throw new SystemException("invalid result from function");
        }
    }

    @Override
    public LimitedApiScopeApproval approve(ClientDetails client, Collection<String> scopes) {
        logger.debug("approve client {} with scopes {}", String.valueOf(client.getClientId()), String.valueOf(scopes));

        if (scopes == null || scopes.isEmpty() || !scopes.contains(scope.getScope())) {
            return null;
        }

        if (executionService == null) {
            logger.trace("execution service is null");
            return null;
        }
        if (!StringUtils.hasText(functionCode)) {
            logger.trace("function code is null");
            return null;
        }

        try {
            // translate client and scopes to a map
            Map<String, Serializable> map = new HashMap<>();
            map.put("scopes", new ArrayList<>(scopes));
            map.put("client", mapper.convertValue(client, serMapTypeRef));

            // execute script
            Map<String, Serializable> customClaims = executionService.executeFunction(functionName, functionCode, map);

            // we expect result to be compatible with approval
            @SuppressWarnings("unchecked")
            ApprovalResult result = mapper.convertValue(customClaims, ApprovalResult.class);
            if (result.approved == null) {
                return null;
            }

            int expiresIn = duration;
            if (result.expiresAt != null) {
                Date now = new Date();
                expiresIn = (int) Math.abs(result.expiresAt.getTime() - now.getTime());
            }

            ApprovalStatus approvalStatus = result.approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED;

            logger.debug(
                "approve client {} with scopes {}: {}",
                client.getClientId(),
                String.valueOf(scopes),
                approvalStatus
            );

            return new LimitedApiScopeApproval(
                scope.getResourceId(),
                scope.getScope(),
                client.getClientId(),
                client.getClientId(),
                expiresIn,
                approvalStatus
            );
        } catch (Exception e) {
            throw new SystemException("invalid result from function");
        }
    }

    @Override
    public String getRealm() {
        return realm;
    }

    /*
     * Function result model
     */

    public class ApprovalResult {

        public Boolean approved;
        public Date expiresAt;
    }
}
