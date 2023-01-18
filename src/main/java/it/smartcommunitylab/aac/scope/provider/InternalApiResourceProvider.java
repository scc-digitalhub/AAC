package it.smartcommunitylab.aac.scope.provider;

import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

@Deprecated
public class InternalApiResourceProvider
        extends AbstractResourceProvider<AbstractInternalApiResource, AbstractInternalApiScope> {

    private Map<String, ApiScopeProvider<? extends AbstractInternalApiScope>> providers;

    public InternalApiResourceProvider(AbstractInternalApiResource resource) {
        super(resource.getAuthority(), resource.getProvider(), resource.getRealm(), resource);

        // build all providers eagerly, internal resources are static
        // use a functional builder
        providers = resource.getApiScopes().stream()
                .map(s -> buildScopeProvider(s))
                .collect(Collectors.toMap(p -> p.getScope().getScope(), p -> p));
    }

    @Override
    public ApiScopeProvider<? extends AbstractInternalApiScope> getScopeProvider(String scope)
            throws NoSuchScopeException {
        ApiScopeProvider<? extends AbstractInternalApiScope> sp = providers.get(scope);
        if (sp == null) {
            throw new NoSuchScopeException();
        }

        return sp;
    }

    protected ApiScopeProvider<? extends AbstractInternalApiScope> buildScopeProvider(AbstractInternalApiScope scope) {
        return new InternalApiScopeProvider(scope);
    }
}