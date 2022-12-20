package it.smartcommunitylab.aac.scope.model;

import java.util.Collection;
import it.smartcommunitylab.aac.core.model.Resource;

public interface ApiResource extends Resource {

    public String getApiResourceId();

    public String getName();

    public String getDescription();

    // a resource defines a namespace (as URI)
    public String getNamespace();

    // a resource defines a set of scopes
    public Collection<? extends ApiScope> getScopes();

    default String getResourceId() {
        return getApiResourceId();
    }
}
