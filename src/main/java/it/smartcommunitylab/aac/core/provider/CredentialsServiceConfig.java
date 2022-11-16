package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;

public interface CredentialsServiceConfig<M extends ConfigMap>
        extends ProviderConfig<M, ConfigurableCredentialsProvider> {

    public String getRepositoryId();

}
