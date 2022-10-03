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
    @Column(name = "provider_id", length = 128, unique = true)
    private String provider;

    @NotNull
    @Column(name = "realm", length = 128)
    private String realm;

    @NotNull
    @Column(name = "name", length = 128)
    private String name;

    @Lob
    @Column(name = "title_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, String> titleMap;

    @Lob
    @Column(name = "description_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, String> descriptionMap;

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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public Map<String, String> getTitleMap() {
        return titleMap;
    }

    public void setTitleMap(Map<String, String> titleMap) {
        this.titleMap = titleMap;
    }

    public Map<String, String> getDescriptionMap() {
        return descriptionMap;
    }

    public void setDescriptionMap(Map<String, String> descriptionMap) {
        this.descriptionMap = descriptionMap;
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

}
