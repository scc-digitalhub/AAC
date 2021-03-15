package it.smartcommunitylab.aac.core.base;

import java.util.HashMap;

import it.smartcommunitylab.aac.SystemKeys;

public class ConfigurableProvider extends AbstractConfigurableProvider {

    private String type;
    private boolean enabled;
    private String persistence;

    public ConfigurableProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
        this.configuration = new HashMap<>();
        this.persistence = SystemKeys.PERSISTENCE_LEVEL_NONE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

}
