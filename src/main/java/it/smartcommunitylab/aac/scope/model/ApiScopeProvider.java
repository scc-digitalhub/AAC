package it.smartcommunitylab.aac.scope.model;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.scope.ScopeApprover;

public interface ApiScopeProvider<S extends ApiScope> extends ResourceProvider<S> {

    public S findScopeByScope(String scope);

    public S findScope(String scopeId);

    public S getScope(String scopeId) throws NoSuchScopeException;

    public Collection<S> listScopes();

    // a scope approver evaluates a policy before authorizing the release
    // for example based on subject type (user/client) or authorities, roles,
    // attributes...
    public ScopeApprover<? extends ScopeApproval> getScopeApprover(String scopeId) throws NoSuchScopeException;
}
