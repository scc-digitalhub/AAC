package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeConfigurationProvider;

public abstract class AbstractAttributeConfigurationProvider<M extends AbstractConfigMap, C extends AbstractAttributeProviderConfig<M>>
        extends AbstractConfigurationProvider<M, ConfigurableAttributeProvider, C>
        implements AttributeConfigurationProvider<M, C> {

    public AbstractAttributeConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public ConfigurableAttributeProvider getConfigurable(C providerConfig) {
        ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(providerConfig.getAuthority(),
                providerConfig.getProvider(),
                providerConfig.getRealm());

        cp.setPersistence(providerConfig.getPersistence());
        cp.setEvents(providerConfig.getEvents());

        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));
        cp.setAttributeSets(providerConfig.getAttributeSets());

        // provider config are active by definition
        cp.setEnabled(true);

        return cp;
    }
}
