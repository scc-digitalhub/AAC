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
@Table(name = "identity_services")
public class IdentityServiceEntity implements ProviderEntity {

    @NotNull
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
    private boolean enabled;

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

    @Column(name = "repository_id", length = 128, unique = true)
    private String repositoryId;

    // key-based configuration for persistence
    // converts to json via custom converter
    @Lob
    @Column(name = "configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> configurationMap;

    public IdentityServiceEntity() {

    }

    public IdentityServiceEntity(String providerId) {
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

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Map<String, Serializable> getConfigurationMap() {
        return configurationMap;
    }

    public void setConfigurationMap(Map<String, Serializable> configurationMap) {
        this.configurationMap = configurationMap;
    }

}
