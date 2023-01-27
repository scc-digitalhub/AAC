/*******************************************************************************
 * Copyright 2015-2019 Smart Community Lab, FBK
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.services.persistence;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import it.smartcommunitylab.aac.repository.HashMapBase64Converter;

/**
 * @author raman
 *
 */
@Entity
@Table(name = "service_model", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "realm", "resource" }),
        @UniqueConstraint(columnNames = { "realm", "namespace" }),
})
public class ServiceEntity {

    public static final String ID_PREFIX = "s_";

    @Id
    @Column(name = "service_id", length = 128)
    private String serviceId;

    @NotNull
    @Column(length = 128)
    private String realm;

    @NotNull
    private String namespace;

    @NotNull
    private String resource;
    private String name;

    // TODO i18n
    private String title;
    private String description;

    @Lob
    @Column(name = "claim_mapping")
    @Convert(converter = HashMapBase64Converter.class)
    private Map<String, String> claimMappings;

    @Column(name = "claim_webhook")
    private String claimWebhook;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Map<String, String> getClaimMappings() {
        return claimMappings;
    }

    public void setClaimMappings(Map<String, String> claimMappings) {
        this.claimMappings = claimMappings;
    }

    public String getClaimWebhook() {
        return claimWebhook;
    }

    public void setClaimWebhook(String claimWebhook) {
        this.claimWebhook = claimWebhook;
    }

}
