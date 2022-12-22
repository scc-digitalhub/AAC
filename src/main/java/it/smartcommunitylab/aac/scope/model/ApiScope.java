package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.core.model.Resource;

public interface ApiScope extends Resource {

    // scope definition according to RFC6749
    public String getScope();

    // unique identifier
    public String getScopeId();

    // a scope is associated to a resource
    public String getApiResourceId();

    public String getName();

    public String getDescription();

    // a scope has an optional approval policy associated
    public String getPolicy();

    default String getResourceId() {
        return getScopeId();
    }
}
