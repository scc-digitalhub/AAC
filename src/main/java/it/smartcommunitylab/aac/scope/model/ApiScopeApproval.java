package it.smartcommunitylab.aac.scope.model;

import org.springframework.security.oauth2.provider.approval.Approval;

import it.smartcommunitylab.aac.core.model.Resource;

public interface ApiScopeApproval extends Resource {

    public String getScope();

    // a scope is associated to a resource
    public String getApiResourceId();

    public String getSubject();

    public String getClient();

    public ApprovalStatus getStatus();

    // TODO remove when possible
    public Approval getApproval();

    public boolean isApproved();

    public long expiresIn();

}
