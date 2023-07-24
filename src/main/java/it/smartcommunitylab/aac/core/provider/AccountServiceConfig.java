package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.model.PersistenceMode;

public interface AccountServiceConfig<M extends ConfigMap> extends ProviderConfig<M> {
    public String getRepositoryId();

    public PersistenceMode getPersistence();
}
