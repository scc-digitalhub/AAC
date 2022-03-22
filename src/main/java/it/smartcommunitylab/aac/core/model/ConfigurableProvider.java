package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorBinding
public class ConfigurableProvider implements ConfigurableProperties {

    @NotBlank
    private String authority;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String realm;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String provider;

    @NotBlank
    private String type;
    private boolean enabled;
    private Boolean registered;

    private String name;
    private String description;

    protected Map<String, Serializable> configuration;

    protected JsonSchema schema;

    public ConfigurableProvider(String authority, String provider, String realm, String type) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
        this.type = type;
        this.configuration = new HashMap<>();
        this.name = provider;
        this.enabled = true;
        this.registered = null;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableProvider() {
        this((String) null, (String) null, (String) null, (String) null);
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        this.configuration = new HashMap<>();
        if (props != null) {
            this.configuration.putAll(props);
        }
    }

    public Serializable getConfigurationProperty(String key) {
        return configuration.get(key);
    }

    public void setConfigurationProperty(String key, Serializable value) {
        configuration.put(key, value);
    }

    public JsonSchema getSchema() {
        return schema;
    }

    public void setSchema(JsonSchema schema) {
        this.schema = schema;
    }

    public Boolean getRegistered() {
        return registered;
    }

    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    public static final String TYPE_IDENTITY = SystemKeys.RESOURCE_IDENTITY;

    public static final String TYPE_ATTRIBUTES = SystemKeys.RESOURCE_ATTRIBUTES;

}
