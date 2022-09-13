package it.smartcommunitylab.aac.core.persistence;

import it.smartcommunitylab.aac.core.model.Resource;

public interface ProviderEntity extends Resource {

    public String getName();

    public String getDescription();

    public boolean isEnabled();

    default public String getId() {
        return getProvider();
    }
}
