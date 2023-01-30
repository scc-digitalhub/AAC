package it.smartcommunitylab.aac.services.model;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.SubjectType;
import it.smartcommunitylab.aac.scope.base.AbstractApiScope;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiServiceScope extends AbstractApiScope {

    public ApiServiceScope(String serviceId) {
        super(SystemKeys.AUTHORITY_SERVICE, serviceId);
        this.serviceId = serviceId;
    }

    @NotBlank
    private String scope;

    @Size(max = 128)
    private String scopeId;

    @Size(max = 128)
    private String serviceId;

    /*
     * Approval policy as exploded criteria
     * TODO refactor
     */

    // require subject type
    private SubjectType approvalType;

    // require realm roles
    private Set<String> approvalRoles;

    // require evaluation of approval function
    @JsonIgnore
    private String approvalFunction;

    // require explicit approval
    private Boolean approvalRequired = false;

    // require anyOf or allOf if more than one criteria
    private Boolean approvalAny = true;

    // compact policy representation
    private String policy;

    @Override
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public String getResourceId() {
        return serviceId;
    }

    @Override
    public String getId() {
        return scopeId;
    }

    // policy
    @Override
    public String getPolicy() {
        if (policy == null) {
            policy = buildPolicy();
        }

        return policy;
    }

    protected void setPolicy(String policy) {
        this.policy = policy;
    }

    public SubjectType getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(SubjectType approvalType) {
        this.approvalType = approvalType;
    }

    public Set<String> getApprovalRoles() {
        return approvalRoles;
    }

    public void setApprovalRoles(Set<String> approvalRoles) {
        this.approvalRoles = approvalRoles;
    }

    public String getApprovalFunction() {
        return approvalFunction;
    }

    public void setApprovalFunction(String approvalFunction) {
        this.approvalFunction = approvalFunction;
    }

    @JsonProperty("approvalFunction")
    public String getApprovalFunctionBase64() {
        if (approvalFunction == null) {
            return null;
        }

        return Base64.getEncoder().encodeToString(approvalFunction.getBytes());
    }

    @JsonProperty("approvalFunction")
    public void setApprovalFunctionBase64(String approvalFunction) {
        if (approvalFunction != null) {
            this.approvalFunction = new String(Base64.getDecoder().decode(approvalFunction.getBytes()));
        }
    }

    public boolean isApprovalRequired() {
        return approvalRequired;
    }

    public Boolean getApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(Boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public boolean isApprovalAny() {
        return approvalAny;
    }

    public Boolean getApprovalAny() {
        return approvalAny;
    }

    public void setApprovalAny(Boolean approvalAny) {
        this.approvalAny = approvalAny;
    }

    /*
     * builder
     */

    protected String buildPolicy() {
        String policy = null;
        // build a composite policy where required
        List<String> policies = new ArrayList<>();
        if (approvalRoles != null) {
            // build policy for authorities
            StringBuilder sb = new StringBuilder();
            sb.append("roles(");
            sb.append(StringUtils.collectionToCommaDelimitedString(approvalRoles));
            sb.append(")");
            policies.add(sb.toString());
        }

        if (approvalType != null) {
            // build policy for type
            StringBuilder sb = new StringBuilder();
            sb.append("subjectType(");
            sb.append(approvalType.getValue());
            sb.append(")");
            policies.add(sb.toString());
        }

        if (approvalFunction != null) {
            // build policy for function
            StringBuilder sb = new StringBuilder();
            sb.append("script(");
            sb.append(getApprovalFunctionBase64());
            sb.append(")");
            policies.add(sb.toString());
        }

        if (approvalRequired != null && approvalRequired) {
            // build policy for store approval
            policies.add("store()");
        }

        // build manually
        // TODO refactor via (fluent) builder
        if (policies.size() == 1) {
            policy = policies.get(0);
        } else if (policies.size() > 1) {
            StringBuilder sb = new StringBuilder();
            String criteria = approvalAny != null && approvalAny ? "anyOf" : "allOf";
            sb.append(criteria).append("(");
            sb.append(StringUtils.collectionToCommaDelimitedString(policies));
            sb.append(")");
            policy = sb.toString();
        }

        return policy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((scopeId == null) ? 0 : scopeId.hashCode());
        result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ApiServiceScope other = (ApiServiceScope) obj;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        if (scopeId == null) {
            if (other.scopeId != null)
                return false;
        } else if (!scopeId.equals(other.scopeId))
            return false;
        if (serviceId == null) {
            if (other.serviceId != null)
                return false;
        } else if (!serviceId.equals(other.serviceId))
            return false;
        return true;
    }

//    public static ServiceScope from(String serviceId, ServiceScopeEntity entity) {
//        ServiceScope scope = new ServiceScope();
//        scope.scope = entity.getScope();
//        scope.serviceId = entity.getServiceId();
//        scope.resourceId = resourceId;
////        scope.audience = Set.of(entity.getServiceId(), namespace);
//        scope.name = entity.getName();
//        scope.description = entity.getDescription();
////        scope.type = entity.getType() != null ? ScopeType.parse(entity.getType()) : ScopeType.GENERIC;
//        scope.claims = StringUtils.commaDelimitedListToSet(entity.getClaims());
//        scope.approvalRoles = StringUtils.commaDelimitedListToSet(entity.getApprovalRoles());
//        scope.approvalSpaceRoles = StringUtils.commaDelimitedListToSet(entity.getApprovalSpaceRoles());
//        scope.approvalFunction = entity.getApprovalFunction();
//        scope.approvalRequired = entity.isApprovalRequired();
//        scope.approvalAny = entity.isApprovalAny();
//
//        return scope;
//    }

}
