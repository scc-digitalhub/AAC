package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfigurationProvider;

public abstract class AbstractIdentityConfigurationProvider<M extends AbstractConfigMap, C extends AbstractIdentityProviderConfig<M>>
        extends AbstractConfigurationProvider<M, ConfigurableIdentityProvider, C>
        implements IdentityProviderConfigurationProvider<M, C> {

    public AbstractIdentityConfigurationProvider(String authority) {
        super(authority);
    }

}
