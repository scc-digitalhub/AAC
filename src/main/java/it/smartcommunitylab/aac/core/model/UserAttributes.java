package it.smartcommunitylab.aac.core.model;

import java.util.Map;

public interface UserAttributes {

    public String getAuthority();

    public String getProvider();

    public Map<String, String> getAttributes();

}
