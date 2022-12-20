package it.smartcommunitylab.aac.scope;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.scope.model.Scope;

/*
 * A registry for scopes
 * 
 * We don't expect providers to be immutable:
 * the registry implementations are supposed to retrieve scopes from providers at each request.
 * 
 * We expect implementations to enforce the existence of a single provider for a given resourceId
 */

public interface ScopeRegistry {
    /*
     * Scope providers
     */
    public void registerScopeProvider(ScopeProvider sp);

    public void unregisterScopeProvider(ScopeProvider sp);

    public ScopeProvider findScopeProvider(String resourceId);

    public ScopeProvider getScopeProviderFromScope(String scope) throws NoSuchScopeException;

    public Collection<ScopeProvider> listScopeProviders();

    /*
     * Scopes as exposed by providers
     */

    public Scope findScope(String scope);

    public Scope getScope(String scope) throws NoSuchScopeException;

    public Collection<Scope> listScopes();

    public Collection<Scope> listScopes(String resourceId);

    /*
     * Approvers are exposed by providers
     */
    public ScopeApprover getScopeApprover(String scope) throws NoSuchScopeException;

    /*
     * Resources as exposed by providers
     */
    public ApiResource findResource(String resourceId);

    public ApiResource getResource(String resourceId) throws NoSuchResourceException;

    public Collection<ApiResource> listResources();

}
