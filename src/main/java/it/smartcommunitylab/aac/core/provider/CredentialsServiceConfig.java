package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;

public interface CredentialsServiceConfig<M extends ConfigMap>
        extends ProviderConfig<M, ConfigurableCredentialsService> {

    public String getRepositoryId();

}
