package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import java.util.Set;

public interface AttributeProviderConfig<T extends ConfigMap> extends ProviderConfig<T> {
    public String getPersistence();

    public String getEvents();

    public Set<String> getAttributeSets();
}
