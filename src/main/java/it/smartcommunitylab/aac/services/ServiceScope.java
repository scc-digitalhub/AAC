package it.smartcommunitylab.aac.services;

import java.util.Set;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeEntity;

public class ServiceScope extends Scope {

    private String serviceId;

    private Set<String> claims;
    private Set<String> approvalRoles;
    private Set<String> approvalSpaceRoles;
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
