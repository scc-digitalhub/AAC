package it.smartcommunitylab.aac.scope.base;

import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.claims.base.AbstractClaimsSetExtractor;
import it.smartcommunitylab.aac.claims.extractors.NullClaimsSetExtractor;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

public abstract class AbstractInternalResourceProvider<R extends AbstractInternalApiResource<S, D>, S extends AbstractInternalApiScope, D extends AbstractClaimDefinition, C extends AbstractApiResourceProviderConfig<R, InternalApiResourceProviderConfigMap>>
        extends AbstractResourceProvider<R, S, InternalApiResourceProviderConfigMap, C> {

    protected Map<String, ApiScopeProvider<S>> providers;
    protected AbstractClaimsSetExtractor extractor;

    protected AbstractInternalResourceProvider(String authority, String provider, String realm, C providerConfig) {
        super(authority, provider, realm, providerConfig);

        // build all providers eagerly, internal resources are static
        providers = resource.getScopes().stream()
                .map(s -> buildScopeProvider(s))
                .collect(Collectors.toMap(p -> p.getScope().getScope(), p -> p));

        extractor = buildClaimsExtractor(resource);

    }

    protected abstract ApiScopeProvider<S> buildScopeProvider(S scope);

    protected AbstractClaimsSetExtractor buildClaimsExtractor(R resource) {
        // use a null extractor by default
        return new NullClaimsSetExtractor(resource.getAuthority(), resource.getProvider(), resource.getRealm(),
                resource.getResource());
    }

    @Override
    public ApiScopeProvider<S> getScopeProvider(String scope) throws NoSuchScopeException {
        ApiScopeProvider<S> sp = providers.get(scope);
        if (sp == null) {
            throw new NoSuchScopeException();
        }

        return sp;
    }

    @Override
    public AbstractClaimsSetExtractor getClaimsExtractor() {
        return extractor;
    }

    protected Map<String, ApiScopeProvider<S>> getProviders() {
        return providers;
    }

    protected void setProviders(Map<String, ApiScopeProvider<S>> providers) {
        this.providers = providers;
    }

    protected AbstractClaimsSetExtractor getExtractor() {
        return extractor;
    }

    protected void setExtractor(AbstractClaimsSetExtractor extractor) {
        this.extractor = extractor;
    }

}
