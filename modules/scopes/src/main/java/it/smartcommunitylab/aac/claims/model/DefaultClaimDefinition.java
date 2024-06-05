package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.model.AttributeType;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Valid
public class DefaultClaimDefinition implements ClaimDefinition {

    @NotBlank
    private String key;

    @NotNull
    private AttributeType attributeType;

    private Boolean isMultiple;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public AttributeType getType() {
        return attributeType;
    }

    public void setAttributeType(AttributeType attributeType) {
        this.attributeType = attributeType;
    }

    public Boolean getIsMultiple() {
        return isMultiple;
    }

    public void setIsMultiple(Boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    @Override
    public boolean isMultiple() {
        return isMultiple != null ? isMultiple.booleanValue() : false;
    }
}
