package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.model.AttributeType;

public class BooleanAttribute extends AbstractAttribute {

    private Boolean value;

    public BooleanAttribute(String key, Boolean boo) {
        this.key = key;
        this.value = boo;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.BOOLEAN;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

}
