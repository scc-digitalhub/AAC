package it.smartcommunitylab.aac.core.base;

import java.util.Map;

import javax.persistence.Convert;

import it.smartcommunitylab.aac.repository.HashMapConverter;

public abstract class AbstractConfigurableProvider extends AbstractProvider {

    protected AbstractConfigurableProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected Map<String, String> configuration;

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    protected String getConfigurationProperty(String key) {
        return configuration.get(key);
    }

    protected void setConfigurationProperty(String key, String value) {
        configuration.put(key, value);
    }
}
