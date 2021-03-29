package it.smartcommunitylab.aac.scope;

import java.util.Collection;

/*
 * Scope provider defines a set of scopes related to a service/implementation etc.
 * 
 * The system will fetch scope definitions from providers and populate the registry when needed.
 */
public interface ScopeProvider {

    public String getResourceId();

    public Collection<Scope> getScopes();

}
