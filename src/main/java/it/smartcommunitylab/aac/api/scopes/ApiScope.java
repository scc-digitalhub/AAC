package it.smartcommunitylab.aac.api.scopes;

import java.util.Set;

import it.smartcommunitylab.aac.scope.Scope;

public abstract class ApiScope extends Scope {

    @Override
    public String getResourceId() {
        return ApiResource.RESOURCE_ID;
    }

    public abstract Set<String> getAuthorities();

}
