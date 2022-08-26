package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public interface IdentityConfigurationProvider<C extends AbstractProviderConfig<P>, P extends ConfigMap>
        extends ConfigurationProvider<ConfigurableIdentityProvider, C, P> {

    default public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

}
