package it.smartcommunitylab.aac.attributes.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.core.model.Attribute;

public abstract class AbstractAttribute implements Attribute, Serializable {
    protected String key;

    protected String name;
    protected String description;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
