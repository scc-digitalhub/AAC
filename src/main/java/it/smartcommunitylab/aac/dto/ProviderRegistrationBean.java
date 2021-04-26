package it.smartcommunitylab.aac.dto;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
public class ProviderRegistrationBean {

    @NotBlank
    private String authority;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String realm;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String provider;

    @NotBlank
    private String type;
    private boolean enabled = false;

    @Size(max = 30)
    private String name;
    
    //TODO add persistence level

    private Map<String, Object> configuration = new HashMap<>();

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
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

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

}
