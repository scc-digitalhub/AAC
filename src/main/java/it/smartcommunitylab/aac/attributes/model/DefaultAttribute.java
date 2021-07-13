package it.smartcommunitylab.aac.attributes.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.model.AttributeType;

public class DefaultAttribute implements Attribute {
    private String key;
    private AttributeType type;
    private String name;
    private String description;
    private Boolean isMultiple;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
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

    public Boolean getIsMultiple() {
        return isMultiple;
    }

    public void setIsMultiple(Boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    @Override
    public Serializable getValue() {
        return null;
    }

}
