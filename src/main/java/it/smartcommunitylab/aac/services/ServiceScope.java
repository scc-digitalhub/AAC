package it.smartcommunitylab.aac.services;

import java.util.Base64;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.model.Scope;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeEntity;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceScope extends Scope {

    @Size(max = 128)
    private String serviceId;

    private Set<String> claims;
    private Set<String> approvalRoles;
    private Set<String> approvalSpaceRoles;
    @JsonIgnore
    private String approvalFunction;

    private boolean approvalRequired = false;
    private boolean approvalAny = true;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Set<String> getClaims() {
        return claims;
    }

    public void setClaims(Set<String> claims) {
        this.claims = claims;
    }

    public Set<String> getApprovalRoles() {
        return approvalRoles;
    }

    public void setApprovalRoles(Set<String> approvalRoles) {
        this.approvalRoles = approvalRoles;
    }

    public Set<String> getApprovalSpaceRoles() {
        return approvalSpaceRoles;
    }

    public void setApprovalSpaceRoles(Set<String> approvalSpaceRoles) {
        this.approvalSpaceRoles = approvalSpaceRoles;
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

    public void setApprovalRequired(boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public boolean isApprovalAny() {
        return approvalAny;
    }

    public void setApprovalAny(boolean approvalAny) {
        this.approvalAny = approvalAny;
    }

    /*
     * builder
     */
    public static ServiceScope from(ServiceScopeEntity entity, String namespace) {
        ServiceScope scope = new ServiceScope();
        scope.scope = entity.getScope();
        scope.serviceId = entity.getServiceId();
        scope.resourceId = namespace;
        scope.audience = Set.of(entity.getServiceId(), namespace);
        scope.name = entity.getName();
        scope.description = entity.getDescription();
        scope.type = entity.getType() != null ? ScopeType.parse(entity.getType()) : ScopeType.GENERIC;
        scope.claims = StringUtils.commaDelimitedListToSet(entity.getClaims());
        scope.approvalRoles = StringUtils.commaDelimitedListToSet(entity.getApprovalRoles());
        scope.approvalSpaceRoles = StringUtils.commaDelimitedListToSet(entity.getApprovalSpaceRoles());
        scope.approvalFunction = entity.getApprovalFunction();
        scope.approvalRequired = entity.isApprovalRequired();
        scope.approvalAny = entity.isApprovalAny();

        return scope;
    }
}
