package it.smartcommunitylab.aac.core.provider;

import java.util.Set;

import it.smartcommunitylab.aac.core.model.ConfigMap;

public interface AttributeProviderConfig<T extends ConfigMap> extends ProviderConfig<T> {

    public String getPersistence();

    public String getEvents();

    public Set<String> getAttributeSets();

}
