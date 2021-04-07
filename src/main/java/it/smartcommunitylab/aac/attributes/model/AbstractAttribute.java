package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.core.model.Attribute;

public abstract class AbstractAttribute implements Attribute {
    protected String key;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
