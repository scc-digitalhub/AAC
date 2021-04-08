package it.smartcommunitylab.aac.services;

import java.util.Set;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.services.persistence.ServiceScopeEntity;

public class ServiceScope extends Scope {

    private String serviceId;

    private Set<String> claims;
    private Set<String> roles;
    private boolean approvalRequired = false;

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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public boolean isApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
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
        scope.roles = StringUtils.commaDelimitedListToSet(entity.getRoles());
        scope.approvalRequired = entity.isApprovalRequired();

        return scope;
    }
}
