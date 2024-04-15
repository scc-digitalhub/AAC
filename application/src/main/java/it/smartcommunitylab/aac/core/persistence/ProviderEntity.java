/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.persistence;

import it.smartcommunitylab.aac.repository.HashMapConverter;
import java.io.Serializable;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/*
 * Provider configuration persistence model
 * Stores generic config + settings + configuration map
 */

@Entity
@Table(name = "providers")
public class ProviderEntity {

    /*
     * Provider details
     */

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
    @Column(name = "type", length = 128)
    private String type;

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

    /*
     * Configuration
     * key-based configuration for persistence
     */

    // settings are mapped per-provider general type
    // converts to json via custom converter
    @Lob
    @Column(name = "settings_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> settingsMap;

    //configuration is mapped per-provider specific type
    // converts to json via custom converter
    @Lob
    @Column(name = "configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> configurationMap;

    @Column(name = "version")
    private Integer version;

    /*
     * Status
     */

    @NotNull
    @Column(name = "enabled")
    private Boolean enabled;

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

    public Map<String, Serializable> getSettingsMap() {
        return settingsMap;
    }

    public void setSettingsMap(Map<String, Serializable> settingsMap) {
        this.settingsMap = settingsMap;
    }

    public Map<String, Serializable> getConfigurationMap() {
        return configurationMap;
    }

    public void setConfigurationMap(Map<String, Serializable> configurationMap) {
        this.configurationMap = configurationMap;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return (
            "ProviderEntity [authority=" +
            authority +
            ", provider=" +
            provider +
            ", realm=" +
            realm +
            ", type=" +
            type +
            ", name=" +
            name +
            ", titleMap=" +
            titleMap +
            ", descriptionMap=" +
            descriptionMap +
            ", version=" +
            version +
            ", enabled=" +
            enabled +
            "]"
        );
    }
}
