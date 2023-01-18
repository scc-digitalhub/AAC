package it.smartcommunitylab.aac.scope.model;

import org.springframework.lang.Nullable;

import it.smartcommunitylab.aac.core.model.Resource;

/*
 * A scope defines a logical restriction on the access to a given resource
 */
public interface Scope extends Resource {

    // unique identifier
    public String getScopeId();

    // scope definition according to RFC6749
    public String getScope();

    // resource identifier
    public String getResourceId();

    // user presentation
    // TODO i18n
    public String getName();

    public String getDescription();

    // a scope has an optional approval policy associated
    public @Nullable String getPolicy();

}
