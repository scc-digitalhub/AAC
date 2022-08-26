package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public interface AttributeConfigurationProvider<C extends AttributeProviderConfig<P>, P extends ConfigMap>
        extends ConfigurationProvider<ConfigurableAttributeProvider, C, P> {

    default public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

}
