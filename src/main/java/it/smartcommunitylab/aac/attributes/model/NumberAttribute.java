package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;
import java.text.ParseException;

public class NumberAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

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

    public static Number parseValue(Serializable value) throws ParseException {
        if (value instanceof Number) {
            return (Number) value;
        }

        String stringValue = String.valueOf(value);

        // parse numbers as float
        return Float.valueOf(stringValue);
    }
}
