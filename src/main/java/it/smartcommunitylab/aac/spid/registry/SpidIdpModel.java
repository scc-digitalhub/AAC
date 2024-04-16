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
