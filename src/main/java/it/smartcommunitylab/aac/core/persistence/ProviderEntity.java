package it.smartcommunitylab.aac.core.persistence;

import java.io.Serializable;
import java.util.Map;

import it.smartcommunitylab.aac.core.model.Resource;

public interface ProviderEntity extends Resource {

    public String getName();

    public String getDescription();

    public boolean isEnabled();

    default public String getId() {
        return getProvider();
    }

    public void setAuthority(String authority);

    public void setProvider(String provider);

    public void setRealm(String realm);

    public void setConfigurationMap(Map<String, Serializable> configurationMap);

}
