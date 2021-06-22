package it.smartcommunitylab.aac.attributes.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.core.model.Attribute;

public abstract class AbstractAttribute implements Attribute, Serializable {
    protected String key;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
