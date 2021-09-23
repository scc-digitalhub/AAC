package it.smartcommunitylab.aac.attributes.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableAttributeProvider;

public class ScriptAttributeProviderConfig extends AbstractConfigurableProvider {

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private String name;
    private String description;

    private String persistence;
    private Set<String> attributeSets;

    // map capabilities
    private ScriptAttributeProviderConfigMap configMap;

    public ScriptAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_SCRIPT, provider, realm);
        this.configMap = new ScriptAttributeProviderConfigMap();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public Set<String> getAttributeSets() {
        return attributeSets;
    }

    public void setAttributeSets(Set<String> attributeSets) {
        this.attributeSets = attributeSets;
    }

    public ScriptAttributeProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(ScriptAttributeProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new ScriptAttributeProviderConfigMap();
        configMap.setConfiguration(props);
    }

    /*
     * builders
     */
    public static ConfigurableAttributeProvider toConfigurableProvider(ScriptAttributeProviderConfig ap) {
        ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(SystemKeys.AUTHORITY_SCRIPT,
                ap.getProvider(),
                ap.getRealm());

        cp.setName(ap.getName());
        cp.setDescription(ap.getDescription());

        cp.setPersistence(ap.getPersistence());
        cp.setAttributeSets(ap.getAttributeSets());

        return cp;
    }

    public static ScriptAttributeProviderConfig fromConfigurableProvider(ConfigurableAttributeProvider cp) {
        ScriptAttributeProviderConfig ap = new ScriptAttributeProviderConfig(cp.getProvider(), cp.getRealm());
        ap.configMap = new ScriptAttributeProviderConfigMap();
        ap.configMap.setConfiguration(cp.getConfiguration());

        ap.name = cp.getName();
        ap.description = cp.getDescription();

        ap.persistence = cp.getPersistence();
        ap.attributeSets = cp.getAttributeSets() != null ? cp.getAttributeSets() : Collections.emptySet();
        return ap;
    }

}
