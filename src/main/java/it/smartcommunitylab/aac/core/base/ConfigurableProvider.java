package it.smartcommunitylab.aac.core.base;

import java.util.HashMap;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
public class ConfigurableProvider extends AbstractConfigurableProvider {

    @NotBlank
    private String type;
    private boolean enabled;
    private String persistence;
    private String name;

    public ConfigurableProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
        this.configuration = new HashMap<>();
        this.persistence = SystemKeys.PERSISTENCE_LEVEL_NONE;
        this.name = provider;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableProvider() {
        this((String) null, (String) null, (String) null);
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

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
    }

    public String getConfigurationProperty(String key) {
        return configuration.get(key);
    }

    public void setConfigurationProperty(String key, String value) {
        configuration.put(key, value);
    }

    public static final String TYPE_IDENTITY = SystemKeys.RESOURCE_IDENTITY;

    public static final String TYPE_ATTRIBUTES = SystemKeys.RESOURCE_ATTRIBUTES;
}
