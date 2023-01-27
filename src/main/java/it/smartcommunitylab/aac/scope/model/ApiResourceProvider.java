package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.claims.model.ClaimsSetExtractor;
import it.smartcommunitylab.aac.claims.model.ClaimsSet;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;

public interface ApiResourceProvider<R extends ApiResource> extends ResourceProvider<R> {

//    public R resolveResource(String resource);
//
//    public R findResource(String resourceId);
//
//    public R getResource(String resourceId) throws NoSuchResourceException;
//
//    public Collection<R> listResources();

    public R getResource();

    public ApiScopeProvider<? extends Scope> getScopeProvider(String scope) throws NoSuchScopeException;
    
    public ClaimsSetExtractor<? extends ClaimsSet> getClaimsExtractor();

}
