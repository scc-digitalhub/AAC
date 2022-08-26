package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;

public interface ProviderConfig<T extends ConfigMap> {
    public String getAuthority();

    public String getRealm();

    public String getProvider();

    public String getName();

    public String getDescription();

    public T getConfigMap();
}
