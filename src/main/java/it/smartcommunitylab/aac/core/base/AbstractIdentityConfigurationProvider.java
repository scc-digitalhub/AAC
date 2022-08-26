package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityConfigurationProvider;

public abstract class AbstractIdentityConfigurationProvider<C extends AbstractProviderConfig<P>, P extends AbstractConfigMap>
        extends AbstractConfigurationProvider<ConfigurableIdentityProvider, C, P>
        implements IdentityConfigurationProvider<C, P> {

    public AbstractIdentityConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public C getConfig(ConfigurableIdentityProvider cp) {
        return super.getConfig(cp);
    }

    @Override
    public C getConfig(ConfigurableIdentityProvider cp, boolean mergeDefault) {
        return super.getConfig(cp, mergeDefault);
    }

}
