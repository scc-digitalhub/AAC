package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfigurationProvider;

public abstract class AbstractIdentityConfigurationProvider<M extends AbstractConfigMap, C extends AbstractIdentityProviderConfig<M>>
        extends AbstractConfigurationProvider<M, ConfigurableIdentityProvider, C>
        implements IdentityProviderConfigurationProvider<M, C> {

    public AbstractIdentityConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public ConfigurableIdentityProvider getConfigurable(C providerConfig) {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(providerConfig.getAuthority(),
                providerConfig.getProvider(),
                providerConfig.getRealm());

        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        cp.setLinkable(providerConfig.getLinkable());
        String persistenceValue = providerConfig.getPersistence() != null ? providerConfig.getPersistence().getValue()
                : null;
        cp.setPersistence(persistenceValue);
        cp.setEvents(providerConfig.getEvents());
        cp.setPosition(providerConfig.getPosition());

        cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));
        cp.setHookFunctions(providerConfig.getHookFunctions());

        // provider config are active by definition
        cp.setEnabled(true);

        return cp;
    }

}
