package it.smartcommunitylab.aac.scope.base;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimsSet;
import it.smartcommunitylab.aac.claims.model.ClaimsExtractor;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.scope.model.ApiResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;

public abstract class AbstractResourceProvider<R extends AbstractApiResource<S>, S extends AbstractApiScope, M extends AbstractConfigMap, C extends AbstractApiResourceProviderConfig<R, M>>
        extends AbstractConfigurableProvider<R, ConfigurableApiResourceProvider, M, C>
        implements ApiResourceProvider<R> {

    protected final R resource;

    protected AbstractResourceProvider(String authority, String provider, String realm, C providerConfig) {
        super(authority, provider, realm, providerConfig);
        Assert.notNull(providerConfig, "provider config is mandatory");

        Assert.notNull(providerConfig.getResource(), "resource can not be null");

        this.resource = providerConfig.getResource();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_API_RESOURCE;
    }

    @Override
    public R getResource() {
        return resource;
    }

    @Override
    public abstract ApiScopeProvider<S> getScopeProvider(String scope)
            throws NoSuchScopeException;

    @Override
    public abstract ClaimsExtractor<? extends AbstractClaimsSet> getClaimsExtractor();

}
