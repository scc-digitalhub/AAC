package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;

public interface ApiResourceProviderConfig<A extends ApiResource, M extends ConfigMap> extends ProviderConfig<M> {

    public A getResource();

}
