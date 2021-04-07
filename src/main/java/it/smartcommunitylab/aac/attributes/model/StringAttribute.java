package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.model.AttributeType;

public class StringAttribute extends AbstractAttribute {

    private String value;

    public StringAttribute(String key) {
        this.key = key;
    }

    public StringAttribute(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.STRING;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
