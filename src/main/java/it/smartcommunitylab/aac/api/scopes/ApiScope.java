package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.scope.Scope;
import java.util.Set;

public abstract class ApiScope extends Scope {

    @Override
    public String getResourceId() {
        return ApiResource.RESOURCE_ID;
    }

    public abstract Set<String> getAuthorities();
}
