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

import it.smartcommunitylab.aac.repository.HashMapBase64Converter;
import it.smartcommunitylab.aac.repository.HashMapConverter;

@Entity
@Table(name = "identity_providers")
public class IdentityProviderEntity implements ProviderEntity {

    @NotNull
    @Column(name = "authority", length = 128)
    private String authority;

    @Id
    @NotNull
    @Column(name = "provider_id", length = 128, unique = true)
    private String provider;

    @NotNull
    @Column(length = 128)
    private String realm;

    @NotNull
    @Column(name = "enabled")
    private Boolean enabled;

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
    @Column(name = "linkable")
    private Boolean linkable = true;

    @Column(name = "persistence_level", length = 32)
    private String persistence;

    @Column(name = "event_level", length = 32)
    private String events;

    @Column(name = "position")
    private Integer position;

    // key-based configuration for persistence
    // converts to json via custom converter
    @Lob
    @Column(name = "configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> configurationMap;

    @Lob
    @Column(name = "hook_functions")
    @Convert(converter = HashMapBase64Converter.class)
    private Map<String, String> hookFunctions;

    @Column(name = "version")
    private Integer version;

    public IdentityProviderEntity() {

    }

    public IdentityProviderEntity(String providerId) {
        this.provider = providerId;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isEnabled() {
        return enabled != null ? enabled.booleanValue() : false;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLinkable() {
        return linkable != null ? linkable.booleanValue() : false;
    }

    public Boolean getLinkable() {
        return this.linkable;
    }

    public void setLinkable(boolean linkable) {
        this.linkable = linkable;
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

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Map<String, Serializable> getConfigurationMap() {
        return configurationMap;
    }

    public void setConfigurationMap(Map<String, Serializable> configurationMap) {
        this.configurationMap = configurationMap;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    public int getVersion() {
        return version != null ? version : 0;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

}
