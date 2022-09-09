package it.smartcommunitylab.aac.core.provider;

import java.util.Set;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public interface AttributeProviderConfig<T extends ConfigMap> extends ProviderConfig<T, ConfigurableAttributeProvider> {

    public String getPersistence();

    public String getEvents();

    public Set<String> getAttributeSets();

}
