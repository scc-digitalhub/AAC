package it.smartcommunitylab.aac.scope.base;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.base.AbstractClaimsSet;
import it.smartcommunitylab.aac.claims.model.ClaimsExtractor;
import it.smartcommunitylab.aac.claims.model.ClaimsSet;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.scope.model.ApiResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiScopeProvider;

public abstract class AbstractResourceProvider<R extends AbstractApiResource<S>, S extends AbstractApiScope>
        extends AbstractProvider<R>
        implements ApiResourceProvider<R> {

    private final R resource;

    protected AbstractResourceProvider(String authority, String provider, String realm, R resource) {
        super(authority, provider, realm);
        Assert.notNull(resource, "resource can not be null");

        this.resource = resource;
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
