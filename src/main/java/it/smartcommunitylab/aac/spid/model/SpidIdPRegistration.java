package it.smartcommunitylab.aac.spid.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
public class SpidIdPRegistration {
    @NotBlank
    private String entityId;
    @NotBlank
    private String entityName;
    @NotBlank
    private String metadataUrl;
    private String entityLabel;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public void setEntityLabel(String entityLabel) {
        this.entityLabel = entityLabel;
    }
}
