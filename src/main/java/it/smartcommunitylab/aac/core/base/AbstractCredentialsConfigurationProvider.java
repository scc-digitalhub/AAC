package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfig;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfigurationProvider;

public abstract class AbstractCredentialsConfigurationProvider<M extends AbstractConfigMap, C extends CredentialsServiceConfig<M>>
        extends AbstractConfigurationProvider<M, ConfigurableCredentialsProvider, C>
        implements CredentialsServiceConfigurationProvider<M, C> {

    public AbstractCredentialsConfigurationProvider(String authority) {
        super(authority);
    }

}
