package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;

public interface IdentityServiceConfigurationProvider<M extends ConfigMap, C extends IdentityServiceConfig<M>>
        extends ConfigurationProvider<M, ConfigurableIdentityService, C> {

}
