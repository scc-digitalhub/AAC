package it.smartcommunitylab.aac.core.persistence;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.repository.HashMapConverter;

@Entity
@Table(name = "template_providers")
public class TemplateProviderEntity implements ProviderEntity {

    @NotNull
    private String authority;

    @Id
    @NotNull
    @Column(name = "realm", length = 128, unique = true)
    private String realm;

    private String name;
    private String description;

    @NotNull
    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "languages")
    private String languages;

    // key-based configuration for persistence
    // converts to json via custom converter
    @Lob
    @Column(name = "configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> configurationMap;

    public TemplateProviderEntity() {

    }

    public TemplateProviderEntity(String realm) {
        this.realm = realm;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public Map<String, Serializable> getConfigurationMap() {
        return configurationMap;
    }

    public void setConfigurationMap(Map<String, Serializable> configurationMap) {
        this.configurationMap = configurationMap;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_TEMPLATE;
    }

    @Override
    public String getProvider() {
        // one provider per realm by design, reuse id
        return getRealm();
    }

}
