package it.smartcommunitylab.aac.scope;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchScopeException;

public interface ScopeRegistry {

    public void registerScope(Scope s);

    public void unregisterScope(Scope s);

    public Scope findScope(String scope);

    public Scope getScope(String scope) throws NoSuchScopeException;

    public Collection<Scope> listScopes();

    public Collection<Scope> listScopes(String resourceId);
}
