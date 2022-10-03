package it.smartcommunitylab.aac.core.provider;

import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;

public interface ProviderConfig<M extends ConfigMap, T extends ConfigurableProvider> extends Resource {

    public String getName();

    public Map<String, String> getTitleMap();

    public Map<String, String> getDescriptionMap();

    public M getConfigMap();

    public T getConfigurable();

    @Override
    default public String getId() {
        return getProvider();
    }

    @Override
    default public String getType() {
        return SystemKeys.RESOURCE_CONFIG;
    }
}
