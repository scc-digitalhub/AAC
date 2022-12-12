package it.smartcommunitylab.aac.core.provider;

import java.util.Map;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

public interface ProviderConfig<M extends ConfigMap, T extends ConfigurableProvider> {

    public String getRealm();

    public String getAuthority();

    public String getProvider();

    public String getName();

    public Map<String, String> getTitleMap();

    public Map<String, String> getDescriptionMap();

    public M getConfigMap();

    public T getConfigurable();

}
