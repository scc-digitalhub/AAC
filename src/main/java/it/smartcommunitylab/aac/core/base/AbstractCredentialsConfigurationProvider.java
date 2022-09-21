package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfig;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfigurationProvider;

public abstract class AbstractCredentialsConfigurationProvider<M extends AbstractConfigMap, C extends CredentialsServiceConfig<M>>
        extends AbstractConfigurationProvider<M, ConfigurableCredentialsService, C>
        implements CredentialsServiceConfigurationProvider<M, C> {

    public AbstractCredentialsConfigurationProvider(String authority) {
        super(authority);
    }

}
