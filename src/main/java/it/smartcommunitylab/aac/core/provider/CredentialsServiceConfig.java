package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;

public interface CredentialsServiceConfig<M extends ConfigMap> extends ProviderConfig<M> {

    public String getRepositoryId();

}
