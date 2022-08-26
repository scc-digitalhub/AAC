package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;
import it.smartcommunitylab.aac.repository.ConfigMapConverter;

public abstract class AbstractProviderConfig<T extends ConfigMap>
        implements ProviderConfig<T>, ConfigurableProperties, Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected static ObjectMapper mapper = new ObjectMapper();
    protected final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private final String authority;
    private final String realm;
    private final String provider;

    protected String name;
    protected String description;

    protected T configMap;

    protected final ConfigMapConverter<T> configMapConverter = new ConfigMapConverter<>();

    protected AbstractProviderConfig(String authority, String provider, String realm, T configMap) {
        Assert.hasText(authority, "authority id is mandatory");

        this.authority = authority;
        this.realm = realm;
        this.provider = provider;

        this.configMap = configMap;
    }

    protected AbstractProviderConfig(ConfigurableProvider cp) {
        this(cp.getAuthority(), cp.getProvider(), cp.getRealm(), null);
        this.name = cp.getName();
        this.description = cp.getDescription();

        // set config
        this.setConfiguration(cp.getConfiguration());
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractProviderConfig() {
        this((String) null, (String) null, (String) null, null);
    }

    public String getAuthority() {
        return authority;
    }

    public String getRealm() {
        return realm;
    }

    public String getProvider() {
        return provider;
    }

    public abstract String getType();

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

    public T getConfigMap() {
        return configMap;
    }

    public void setConfigMap(T configMap) {
        this.configMap = configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(getConfigMap(), typeRef);
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        T map = configMapConverter.convert(props);
        setConfigMap(map);
    }

}
