package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProviderConfig;

public abstract class AbstractAttributeConfigurationProvider<C extends AttributeProviderConfig<P>, P extends AbstractConfigMap>
        extends AbstractConfigurationProvider<ConfigurableAttributeProvider, C, P>
        implements AttributeConfigurationProvider<C, P> {

    public AbstractAttributeConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public C getConfig(ConfigurableAttributeProvider cp) {
        return super.getConfig(cp);
    }

    @Override
    public C getConfig(ConfigurableAttributeProvider cp, boolean mergeDefault) {
        return super.getConfig(cp, mergeDefault);
    }

}
