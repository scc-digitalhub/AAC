package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;

public interface AccountServiceConfigurationProvider<M extends ConfigMap, C extends AccountServiceConfig<M>>
        extends ConfigurationProvider<M, ConfigurableAccountProvider, C> {

}
