package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfig;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfigurationProvider;

public abstract class AbstractCredentialsConfigurationProvider<C extends CredentialsServiceConfig<M>, M extends AbstractConfigMap>
        extends AbstractConfigurationProvider<M, ConfigurableCredentialsService, C>
        implements CredentialsServiceConfigurationProvider<M, C> {

    public AbstractCredentialsConfigurationProvider(String authority) {
        super(authority);
    }

}
