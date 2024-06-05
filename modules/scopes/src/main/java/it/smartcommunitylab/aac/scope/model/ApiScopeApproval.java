package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.model.Resource;

public interface ApiScopeApproval extends Resource {
    // associated to a resource
    public String getApiResourceId();

    // associated to a scope
    public String getScope();

    public String getSubject();

    public String getClient();

    public ApprovalStatus getStatus();

    // TODO remove when possible
    // public Approval getApproval();

    public boolean isApproved();

    public long expiresIn();
}
