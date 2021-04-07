package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.model.AttributeType;

public class NumberAttribute extends AbstractAttribute {

    private Number value;

    public NumberAttribute(String key) {
        this.key = key;
    }

    public NumberAttribute(String key, Number number) {
        this.key = key;
        this.value = number;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.NUMBER;
    }

    @Override
    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

}
