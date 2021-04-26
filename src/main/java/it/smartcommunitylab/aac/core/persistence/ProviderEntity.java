package it.smartcommunitylab.aac.core.persistence;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import it.smartcommunitylab.aac.repository.HashMapConverter;

@Entity
@Table(name = "providers")
public class ProviderEntity {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private String authority;

    @NotNull
    @Column(name = "provider_id", unique = true)
    private String providerId;

    @NotNull
    private String realm;

    @NotNull
    @Column(name = "provider_type")
    private String type;

    @NotNull
    @Column(name = "enabled")
    private boolean enabled;

    private String name;

    // key-based configuration for persistence
    // converts to json via custom converter
    @Lob
    @Column(name = "configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Object> configurationMap;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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
        this.name = name;
    }

    public Map<String, Object> getConfigurationMap() {
        return configurationMap;
    }

    public void setConfigurationMap(Map<String, Object> configurationMap) {
        this.configurationMap = configurationMap;
    }

}
