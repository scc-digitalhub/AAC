package it.smartcommunitylab.aac.oauth.scope;

import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

public class OAuth2DCRResourceProvider extends AbstractResourceProvider<OAuth2DCRResource, AbstractInternalApiScope> {

    private Map<String, ApiScopeProvider<?>> providers;

    protected OAuth2DCRResourceProvider(OAuth2DCRResource resource) {
        super(resource.getAuthority(), resource.getProvider(), resource.getRealm(), resource);

        // build all providers eagerly, internal resources are static
        providers = resource.getApiScopes().stream()
                .map(s -> new OAuth2DCRScopeProvider(s))
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