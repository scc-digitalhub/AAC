package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.scope.ScopeApprover;

public interface ApiScopeProvider<S extends Scope> {
    public S getScope();

    // a scope approver evaluates a policy before authorizing the release
    // for example based on subject type (user/client) or authorities, roles,
    // attributes...
    public ScopeApprover<? extends ApiScopeApproval> getScopeApprover();
}
