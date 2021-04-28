package it.smartcommunitylab.aac.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderRegistrationBean implements ConfigurableProperties {

    @NotBlank
    private String authority;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String realm;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String provider;

    @NotBlank
    private String type;
    private Boolean enabled;

    @Size(max = 30)
    private String name;

    @Size(max = 70)
    private String description;

    private Boolean registered;

    // TODO add persistence level as enum
    private String persistence;

    private Map<String, Serializable> configuration = new HashMap<>();

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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Boolean getRegistered() {
        return registered;
    }

    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    public Map<String, Serializable> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Serializable> configuration) {
        this.configuration = configuration;
    }

//    public Map<String, Serializable> getConfiguration() {
//        return configuration;
//    }
//
//    public void setConfiguration(Map<String, Serializable> configuration) {
//        this.configuration = configuration;
//    }

    public static ProviderRegistrationBean fromProvider(ConfigurableProvider provider) {
        ProviderRegistrationBean reg = new ProviderRegistrationBean();
        reg.authority = provider.getAuthority();
        reg.realm = provider.getRealm();
        reg.provider = provider.getProvider();
        reg.type = provider.getType();

        reg.name = provider.getName();
        reg.description = provider.getDescription();

        reg.enabled = provider.isEnabled();
        reg.persistence = provider.getPersistence();

        reg.configuration = provider.getConfiguration();

        return reg;
    }

}
