package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;

public interface IdentityServiceConfig<M extends ConfigMap> extends ProviderConfig<M, ConfigurableIdentityService> {

    public String getRepositoryId();

}
