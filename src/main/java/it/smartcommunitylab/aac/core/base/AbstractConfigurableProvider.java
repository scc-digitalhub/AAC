package it.smartcommunitylab.aac.core.base;

import java.util.Map;

public abstract class AbstractConfigurableProvider extends AbstractProvider {

    protected AbstractConfigurableProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    // key-based configuration for persistence
    private Map<String, String> configuration;

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public String getConfigurationProperty(String key) {
        return configuration.get(key);
    }

    public void setConfigurationPropety(String key, String value) {
        configuration.put(key, value);
    }
}
