package it.smartcommunitylab.aac.scope.model;

import java.util.Collection;

import it.smartcommunitylab.aac.claims.model.ClaimDefinition;
import it.smartcommunitylab.aac.core.model.Resource;

public interface ApiResource extends Resource {

    // unique identifier
    public String getResourceId();

    // resource indicator as per RFC8707
    public String getResource();

    // a resource defines a namespace (as URI)
    // used for claims when provided
    public String getNamespace();

    // logical name
    public String getName();

    // user presentation
    // TODO translate map?
    public String getTitle();

    public String getDescription();

    // a resource defines a set of scopes
//    public Collection<String> getScopes();

    public Collection<? extends Scope> getScopes();

    // a resource can define a set of claims under its namespace
    public Collection<? extends ClaimDefinition> getClaims();

}
