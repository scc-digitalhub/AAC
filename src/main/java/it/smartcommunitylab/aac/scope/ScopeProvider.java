package it.smartcommunitylab.aac.scope;

import java.util.Collection;

import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.scope.model.Scope;

/*
 * Scope provider defines a set of scopes related to a service/implementation etc.
 * 
 * The system will fetch scope definitions from providers and populate the registry when needed.
 * 
 * Do note that we expect exported scopes to match the resourceId declared from provider
 */
public interface ScopeProvider {

    public String getResourceId();
    
    public ApiResource getResource();

    public Collection<Scope> getScopes();

    public ScopeApprover getApprover(String scope);
}
