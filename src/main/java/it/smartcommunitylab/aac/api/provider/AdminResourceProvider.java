package it.smartcommunitylab.aac.api.provider;

import it.smartcommunitylab.aac.api.scopes.AdminApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractInternalResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

public class AdminResourceProvider extends AbstractInternalResourceProvider<AdminApiResource> {

    public AdminResourceProvider(AdminApiResource resource) {
        super(resource);
    }

    @Override
    protected ApiScopeProvider<AbstractInternalApiScope> buildScopeProvider(AbstractInternalApiScope scope) {
        return new AdminScopeProvider(scope);
    }

}