package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.core.provider.ResourceProvider;

public interface ApiResourceProvider<R extends ApiResource> extends ResourceProvider<R> {

    public R getResource();

    public ApiScopeProvider<? extends ApiScope> getScopeProvider();

}
