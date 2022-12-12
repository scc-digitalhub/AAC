package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;

public interface CredentialsServiceConfigurationProvider<M extends ConfigMap, C extends CredentialsServiceConfig<M>>
        extends ConfigurationProvider<M, ConfigurableCredentialsProvider, C> {

}
