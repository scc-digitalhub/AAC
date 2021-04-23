package it.smartcommunitylab.aac.core.base;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Convert;

import it.smartcommunitylab.aac.repository.HashMapConverter;

public abstract class AbstractConfigurableProvider extends AbstractProvider {

    protected AbstractConfigurableProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected Map<String, Object> configuration = new HashMap<>();

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    protected Object getConfigurationProperty(String key) {
        return configuration.get(key);
    }

    protected void setConfigurationProperty(String key, Object value) {
        configuration.put(key, value);
    }
}
