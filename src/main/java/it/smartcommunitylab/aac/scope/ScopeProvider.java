package it.smartcommunitylab.aac.scope;

import java.util.Collection;

/*
 * Scope provider defines a set of scopes related to a service/implementation etc.
 *
 * The system will fetch scope definitions from providers and populate the registry when needed.
 *
 * Do note that we expect exported scopes to match the resourceId declared from provider
 */
public interface ScopeProvider {
    public String getResourceId();

    public Resource getResource();

    public Collection<Scope> getScopes();

    public ScopeApprover getApprover(String scope);
}
