package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;

public interface IdentityServiceConfig<M extends ConfigMap> extends ProviderConfig<M> {

    public String getRepositoryId();

}
