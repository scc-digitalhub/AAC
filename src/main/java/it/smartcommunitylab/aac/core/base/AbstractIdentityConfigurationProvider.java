package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfigurationProvider;

public abstract class AbstractIdentityConfigurationProvider<C extends AbstractIdentityProviderConfig<M>, M extends AbstractConfigMap>
        extends AbstractConfigurationProvider<M, ConfigurableIdentityProvider, C>
        implements IdentityProviderConfigurationProvider<M, C> {

    public AbstractIdentityConfigurationProvider(String authority) {
        super(authority);
    }

//    @Override
//    public C getConfig(ConfigurableIdentityProvider cp) {
//        return super.getConfig(cp);
//    }
//
//    @Override
//    public C getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
//        return super.getConfig(cp, mergeDefault);
//    }

}
