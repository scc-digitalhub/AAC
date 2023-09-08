/**
 * Copyright 2023 Fondazione Bruno Kessler
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/*
 * Provider configuration persistence model
 * Stores generic config + settings + configuration map
 */

@Entity
@IdClass(ProviderConfigId.class)
@Table(name = "provider_configs")
public class ProviderConfigEntity {

    /*
     * Provider details
     */

    @Id
    @NotNull
    @Column(name = "provider_id", length = 128, unique = true)
    private String providerId;

    @Id
    @NotNull
    @Column(name = "type", length = 128)
    private String type;

    @NotNull
    @Column(length = 128)
    private String realm;

    /*
     * Configuration
     * blob configuration for persistence
     */

    @Lob
    @Column(name = "config")
    private byte[] config;

    @Column(name = "version")
    private Integer version;

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public byte[] getConfig() {
        return config;
    }

    public void setConfig(byte[] config) {
        this.config = config;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return (
            "ProviderConfigEntity [providerId=" +
            providerId +
            ", type=" +
            type +
            ", realm=" +
            realm +
            ", version=" +
            version +
            "]"
        );
    }
}
