package it.smartcommunitylab.aac.core.persistence;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import it.smartcommunitylab.aac.repository.HashMapBase64Converter;
import it.smartcommunitylab.aac.repository.HashMapConverter;

@Entity
@Table(name = "providers")
public class IdentityProviderEntity {

//    @Id
//    @GeneratedValue
//    private Long id;

    @NotNull
    private String authority;

    @Id
    @NotNull
    @Column(name = "provider_id", unique = true)
    private String providerId;

    @NotNull
    private String realm;

//    @NotNull
//    @Column(name = "provider_type")
//    private String type;

    @NotNull
    @Column(name = "enabled")
    private boolean enabled;

    private String name;
    private String description;
    private String icon;

    @NotNull
    @Column(name = "linkable")
    private boolean linkable = true;

    @Column(name = "persistence_level")
    private String persistence;

    @Column(name = "event_level")
    private String events;

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

    @Column(name = "display_mode")
    private String displayMode;

    public IdentityProviderEntity() {

    }

    public IdentityProviderEntity(String providerId) {
        this.providerId = providerId;
    }

//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }

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

//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLinkable() {
        return linkable;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
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

}
