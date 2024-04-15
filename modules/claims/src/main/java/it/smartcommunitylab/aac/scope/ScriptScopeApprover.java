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

package it.smartcommunitylab.aac.scope;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.dto.UserProfile;
import it.smartcommunitylab.aac.users.model.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class ScriptScopeApprover implements ScopeApprover {

    public static final String APPROVAL_FUNCTION = "approver";
    public static final int DEFAULT_DURATION_MS = 3600000; // 1h

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setSerializationInclusion(Include.NON_NULL);
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    private final String realm;
    private final String resourceId;
    private final String scope;
    private int duration;

    private String functionName;
    private String functionCode;
    private ScriptExecutionService executionService;

    public ScriptScopeApprover(String realm, String resourceId, String scope) {
        Assert.hasText(resourceId, "resourceId can not be blank or null");
        Assert.hasText(scope, "scope can not be blank or null");
        this.realm = realm;
        this.resourceId = resourceId;
        this.scope = scope;
        this.duration = DEFAULT_DURATION_MS;
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
    public Approval approveUserScope(String scope, User user, ClientDetails client, Collection<String> scopes)
        throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }
        if (executionService == null) {
            return null;
        }
        if (!StringUtils.hasText(functionCode)) {
            return null;
        }

        // convert to profile beans
        // TODO client
        UserProfile profile = new UserProfile(user);

        // translate user, client and scopes to a map
        Map<String, Serializable> map = new HashMap<>();
        map.put("scopes", new ArrayList<>(scopes));
        map.put("user", mapper.convertValue(profile, serMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));

        // execute script
        Map<String, Serializable> customClaims = executionService.executeFunction(functionName, functionCode, map);

        // we expect result to be compatible with approval
        try {
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
            return new Approval(resourceId, client.getClientId(), scope, expiresIn, approvalStatus);
        } catch (Exception e) {
            throw new SystemException("invalid result from function");
        }
    }

    @Override
    public Approval approveClientScope(String scope, ClientDetails client, Collection<String> scopes)
        throws InvalidDefinitionException, SystemException {
        if (!this.scope.equals(scope)) {
            return null;
        }
        if (executionService == null) {
            return null;
        }
        if (!StringUtils.hasText(functionCode)) {
            return null;
        }

        // translate client and scopes to a map
        Map<String, Serializable> map = new HashMap<>();
        map.put("scopes", new ArrayList<>(scopes));
        map.put("client", mapper.convertValue(client, serMapTypeRef));

        // execute script
        Map<String, Serializable> customClaims = executionService.executeFunction(functionName, functionCode, map);

        // we expect result to be compatible with approval
        try {
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
            return new Approval(resourceId, client.getClientId(), scope, expiresIn, approvalStatus);
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
