package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;

public interface AccountServiceConfig<M extends ConfigMap> extends ProviderConfig<M, ConfigurableAccountService> {

    public String getRepositoryId();

}
