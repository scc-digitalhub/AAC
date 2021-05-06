package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;

public class InternalIdentityProviderConfig extends AbstractConfigurableProvider {

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private String name;
    private String description;

    // map capabilities
    private InternalIdentityProviderConfigMap configMap;

    // hook functions
    private Map<String, String> hookFunctions;

    public InternalIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.configMap = new InternalIdentityProviderConfigMap();
        this.hookFunctions = Collections.emptyMap();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
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

    public void setConfigMap(InternalIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    public InternalIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new InternalIdentityProviderConfigMap();
        configMap.setConfiguration(props);
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    /*
     * builders
     */
    public static ConfigurableProvider toConfigurableProvider(InternalIdentityProviderConfig ip) {
        ConfigurableProvider cp = new ConfigurableProvider(SystemKeys.AUTHORITY_INTERNAL, ip.getProvider(),
                ip.getRealm());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);
        cp.setPersistence(SystemKeys.PERSISTENCE_LEVEL_REPOSITORY);

        cp.setName(ip.getName());
        cp.setDescription(ip.getDescription());

        cp.setEnabled(true);
        cp.setConfiguration(ip.getConfigMap().getConfiguration());
        cp.setHookFunctions(ip.getHookFunctions());

        return cp;
    }

    public static InternalIdentityProviderConfig fromConfigurableProvider(ConfigurableProvider cp) {
        InternalIdentityProviderConfig ip = new InternalIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        ip.configMap = new InternalIdentityProviderConfigMap();
        ip.configMap.setConfiguration(cp.getConfiguration());

        ip.name = cp.getName();
        ip.description = cp.getDescription();
        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return ip;
    }

    public static InternalIdentityProviderConfig fromConfigurableProvider(ConfigurableProvider cp,
            InternalIdentityProviderConfigMap defaultConfigMap) {
        InternalIdentityProviderConfig ip = fromConfigurableProvider(cp);

        // double conversion via map to merge default props
        Map<String, Serializable> config = new HashMap<>();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        Map<String, Serializable> defaultMap = mapper.convertValue(defaultConfigMap, typeRef);
        config.putAll(defaultMap);
        config.putAll(cp.getConfiguration());

        ip.configMap.setConfiguration(config);
        return ip;
    }

}
