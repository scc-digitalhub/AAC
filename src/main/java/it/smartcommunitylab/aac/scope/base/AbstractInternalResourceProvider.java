package it.smartcommunitylab.aac.scope.base;

import java.util.Map;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.claims.base.AbstractClaimsExtractor;
import it.smartcommunitylab.aac.claims.extractors.NullClaimsExtractor;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

public abstract class AbstractInternalResourceProvider<R extends AbstractInternalApiResource, C extends AbstractApiResourceProviderConfig<R, InternalApiResourceProviderConfigMap>>
        extends AbstractResourceProvider<R, AbstractInternalApiScope, InternalApiResourceProviderConfigMap, C> {

    protected Map<String, ApiScopeProvider<AbstractInternalApiScope>> providers;
    protected AbstractClaimsExtractor extractor;

    protected AbstractInternalResourceProvider(String authority, String provider, String realm, C providerConfig) {
        super(authority, provider, realm, providerConfig);

        // build all providers eagerly, internal resources are static
        providers = resource.getScopes().stream()
                .map(s -> buildScopeProvider(s))
                .collect(Collectors.toMap(p -> p.getScope().getScope(), p -> p));

        extractor = buildClaimsExtractor(resource);

    }

    protected abstract ApiScopeProvider<AbstractInternalApiScope> buildScopeProvider(AbstractInternalApiScope scope);

    protected AbstractClaimsExtractor buildClaimsExtractor(R resource) {
        // use a null extractor by default
        return new NullClaimsExtractor<R>(resource.getAuthority(), resource.getProvider(), resource.getRealm(),
                resource.getResource());
    }

    @Override
    public ApiScopeProvider<AbstractInternalApiScope> getScopeProvider(String scope) throws NoSuchScopeException {
        ApiScopeProvider<AbstractInternalApiScope> sp = providers.get(scope);
        if (sp == null) {
            throw new NoSuchScopeException();
        }

        return sp;
    }

    @Override
    public AbstractClaimsExtractor getClaimsExtractor() {
        return extractor;
    }

    protected Map<String, ApiScopeProvider<AbstractInternalApiScope>> getProviders() {
        return providers;
    }

    protected void setProviders(Map<String, ApiScopeProvider<AbstractInternalApiScope>> providers) {
        this.providers = providers;
    }

    protected AbstractClaimsExtractor getExtractor() {
        return extractor;
    }

    protected void setExtractor(AbstractClaimsExtractor extractor) {
        this.extractor = extractor;
    }

}
