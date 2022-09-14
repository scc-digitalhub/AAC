package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProviderConfig;

public abstract class AbstractAttributeConfigurationProvider<C extends AttributeProviderConfig<M>, M extends AbstractConfigMap>
        extends AbstractConfigurationProvider<M, ConfigurableAttributeProvider, C>
        implements AttributeConfigurationProvider<M, C> {

    public AbstractAttributeConfigurationProvider(String authority) {
        super(authority);
    }

}
