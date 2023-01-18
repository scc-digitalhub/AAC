package it.smartcommunitylab.aac.claims.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import it.smartcommunitylab.aac.model.AttributeType;

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
    public AttributeType getAttributeType() {
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
