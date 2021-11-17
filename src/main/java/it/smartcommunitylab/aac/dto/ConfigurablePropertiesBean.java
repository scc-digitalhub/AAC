package it.smartcommunitylab.aac.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import it.smartcommunitylab.aac.core.base.ConfigurableProperties;

public class ConfigurablePropertiesBean implements ConfigurableProperties {
    private Map<String, Serializable> configuration = new HashMap<>();

    public Map<String, Serializable> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Serializable> configuration) {
        this.configuration = configuration;
    }

}
