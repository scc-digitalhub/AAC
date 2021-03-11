package it.smartcommunitylab.aac.core.base;

import java.util.HashMap;

public class ConfigurableProvider extends AbstractConfigurableProvider {

    private String type;
    private boolean enabled;

    public ConfigurableProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
        this.configuration = new HashMap<>();
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

}
