package it.smartcommunitylab.aac.scope.model;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

public interface ApiResourceProviderConfigurationProvider<A extends ApiResource, M extends ConfigMap, C extends ApiResourceProviderConfig<A, M>>
        extends ConfigurationProvider<M, ConfigurableApiResourceProvider, C> {

}
