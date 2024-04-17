/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpidIdpModel {

    @JsonProperty("ipa_entity_code")
    private String ipaEntityCode;

    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("entity_name")
    private String entityName;

    @JsonProperty("metadata_url")
    private String metadataUrl;

    @JsonProperty("entity_type")
    private String entityType;

    public SpidIdpModel() {}

    public SpidIdpModel(
        String ipaEntityCode,
        String entityId,
        String entityName,
        String metadataUrl,
        String entityType
    ) {
        this.ipaEntityCode = ipaEntityCode;
        this.entityId = entityId;
        this.entityName = entityName;
        this.metadataUrl = metadataUrl;
        this.entityType = entityType;
    }

    public String getIpaEntityCode() {
        return ipaEntityCode;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public String getEntityType() {
        return entityType;
    }

    @Override
    public String toString() {
        return (
            "SpidIdpDataModel{" +
            "ipaEntityCode='" +
            ipaEntityCode +
            '\'' +
            ", entityId='" +
            entityId +
            '\'' +
            ", entityName='" +
            entityName +
            '\'' +
            ", metadataUrl='" +
            metadataUrl +
            '\'' +
            ", entityType='" +
            entityType +
            '\'' +
            '}'
        );
    }
}
