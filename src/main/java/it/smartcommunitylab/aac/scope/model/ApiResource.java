package it.smartcommunitylab.aac.scope.model;

import java.util.Collection;
import it.smartcommunitylab.aac.core.model.Resource;

public interface ApiResource extends Resource {

    //unique identifier
    public String getApiResourceId();

    public String getName();

    public String getDescription();
    
    //resource indicator as per RFC8707
    public String getResource();

    // a resource defines a namespace (as URI)
    // used for claims when provided
    public String getNamespace();

    // a resource defines a set of scopes
    public Collection<? extends ApiScope> getScopes();

    default String getResourceId() {
        return getApiResourceId();
    }
}
