package it.smartcommunitylab.aac.api.scopes;

import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

public class AdminResourceProvider extends AbstractResourceProvider<AdminApiResource, AbstractInternalApiScope> {

    private Map<String, ApiScopeProvider<?>> providers;

    protected AdminResourceProvider(AdminApiResource resource) {
        super(resource.getAuthority(), resource.getProvider(), resource.getRealm(), resource);

        // build all providers eagerly, internal resources are static
        providers = resource.getApiScopes().stream()
                .map(s -> new AdminScopeProvider(s))
                .collect(Collectors.toMap(p -> p.getScope().getScope(), p -> p));
    }

    @Override
    public ApiScopeProvider<?> getScopeProvider(String scope) throws NoSuchScopeException {
        ApiScopeProvider<?> sp = providers.get(scope);
        if (sp == null) {
            throw new NoSuchScopeException();
        }

        return sp;
    }

}