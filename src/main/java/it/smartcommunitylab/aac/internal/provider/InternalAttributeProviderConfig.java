package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableAttributeProvider;

public class InternalAttributeProviderConfig extends AbstractConfigurableProvider {

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private String name;
    private String description;

    // map capabilities
    private InternalAttributeProviderConfigMap configMap;

    public InternalAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.configMap = new InternalAttributeProviderConfigMap();
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

    public InternalAttributeProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(InternalAttributeProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new InternalAttributeProviderConfigMap();
        configMap.setConfiguration(props);
    }

    /*
     * builders
     */
    public static ConfigurableAttributeProvider toConfigurableProvider(InternalAttributeProviderConfig ap) {
        ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(SystemKeys.AUTHORITY_INTERNAL,
                ap.getProvider(),
                ap.getRealm());

        return cp;
    }

    public static InternalAttributeProviderConfig fromConfigurableProvider(ConfigurableAttributeProvider cp) {
        InternalAttributeProviderConfig ap = new InternalAttributeProviderConfig(cp.getProvider(), cp.getRealm());
        ap.configMap = new InternalAttributeProviderConfigMap();
        ap.configMap.setConfiguration(cp.getConfiguration());

        ap.name = cp.getName();
        ap.description = cp.getDescription();

        return ap;
    }

}
