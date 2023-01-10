package it.smartcommunitylab.aac.profiles.scope;

import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

public class OpenIdUserInfoResourceProvider
        extends AbstractResourceProvider<OpenIdUserInfoResource, AbstractInternalApiScope> {

    private Map<String, ApiScopeProvider<?>> providers;

    protected OpenIdUserInfoResourceProvider(OpenIdUserInfoResource resource) {
        super(resource.getAuthority(), resource.getProvider(), resource.getRealm(), resource);

        // build all providers eagerly, internal resources are static
        providers = resource.getApiScopes().stream()
                .map(s -> new ProfileScopeProvider(s))
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