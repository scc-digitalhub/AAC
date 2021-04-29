package it.smartcommunitylab.aac.scope;

import java.util.Collection;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;

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
