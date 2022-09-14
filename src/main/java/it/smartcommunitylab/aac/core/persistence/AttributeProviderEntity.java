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
@Table(name = "attribute_providers")
public class AttributeProviderEntity implements ProviderEntity {

    @NotNull
    private String authority;

    @Id
    @NotNull
    @Column(name = "provider_id", length = 128, unique = true)
    private String providerId;

    @NotNull
    @Column(length = 128)
    private String realm;

    @NotNull
    @Column(name = "enabled")
    private boolean enabled;

    private String name;
    private String description;

    @Column(name = "persistence_level", length = 32)
    private String persistence;

    @Column(name = "event_level", length = 32)
    private String events;

    @Column(name = "set_ids")
    private String attributeSets;

    // key-based configuration for persistence
    // converts to json via custom converter
    @Lob
    @Column(name = "configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> configurationMap;

    public AttributeProviderEntity() {

    }

    public AttributeProviderEntity(String providerId) {
        this.providerId = providerId;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getProvider() {
        return providerId;
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

    public String getAttributeSets() {
        return attributeSets;
    }

    public void setAttributeSets(String attributeSets) {
        this.attributeSets = attributeSets;
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

    public Map<String, Serializable> getConfigurationMap() {
        return configurationMap;
    }

    public void setConfigurationMap(Map<String, Serializable> configurationMap) {
        this.configurationMap = configurationMap;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTE_PROVIDER;
    }

}
