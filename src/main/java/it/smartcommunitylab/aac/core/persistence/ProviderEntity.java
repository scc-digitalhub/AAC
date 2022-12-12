package it.smartcommunitylab.aac.core.persistence;

import java.io.Serializable;
import java.util.Map;

public interface ProviderEntity {
    public String getRealm();

    public String getAuthority();

    public String getProvider();

    public String getName();

    public Map<String, String> getTitleMap();

    public Map<String, String> getDescriptionMap();

    public boolean isEnabled();

    public void setAuthority(String authority);

    public void setProvider(String provider);

    public void setRealm(String realm);

    public void setConfigurationMap(Map<String, Serializable> configurationMap);

}
