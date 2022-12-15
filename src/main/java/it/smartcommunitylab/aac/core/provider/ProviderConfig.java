package it.smartcommunitylab.aac.core.provider;

import java.util.Map;

import it.smartcommunitylab.aac.core.model.ConfigMap;

public interface ProviderConfig<M extends ConfigMap> {

    public String getRealm();

    public String getAuthority();

    public String getProvider();

    public String getName();

    public Map<String, String> getTitleMap();

    public Map<String, String> getDescriptionMap();

    public M getConfigMap();

    public int getVersion();

}
