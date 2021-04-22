package it.smartcommunitylab.aac.internal.provider;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;

public class InternalIdentityProviderConfig extends AbstractConfigurableProvider {

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
    };

    private String name;

    // map capabilities
    private InternalIdentityProviderConfigMap configMap;

    protected InternalIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.configMap = new InternalIdentityProviderConfigMap();
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

    public void setConfigMap(InternalIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    public InternalIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    @Override
    public Map<String, String> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(configMap, typeRef);
    }

    /*
     * builders
     */
    public static ConfigurableProvider toConfigurableProvider(InternalIdentityProviderConfig ip) {
        ConfigurableProvider cp = new ConfigurableProvider(SystemKeys.AUTHORITY_INTERNAL, ip.getProvider(),
                ip.getRealm());
        cp.setName(ip.getName());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);
        cp.setConfiguration(ip.getConfiguration());

        return cp;
    }

    public static InternalIdentityProviderConfig fromConfigurableProvider(ConfigurableProvider cp) {
        InternalIdentityProviderConfig op = new InternalIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        op.configMap = mapper.convertValue(cp.getConfiguration(), InternalIdentityProviderConfigMap.class);
        op.name = cp.getName();
        return op;

    }

    public static InternalIdentityProviderConfig fromConfigurableProvider(ConfigurableProvider cp,
            InternalIdentityProviderConfigMap defaultConfigMap) {
        InternalIdentityProviderConfig op = new InternalIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        // double conversion via map to merge default props
        Map<String, String> config = new HashMap<>();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        Map<String, String> defaultMap = mapper.convertValue(defaultConfigMap, typeRef);
        config.putAll(defaultMap);
        config.putAll(cp.getConfiguration());

        op.configMap = mapper.convertValue(config, InternalIdentityProviderConfigMap.class);
        op.name = cp.getName();
        return op;

    }

    /*
     * configMap
     */

}
