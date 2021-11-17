package it.smartcommunitylab.aac.attributes.model;

import java.io.Serializable;
import java.text.ParseException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.AttributeType;

public class BooleanAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private Boolean value;

    public BooleanAttribute(String key) {
        this.key = key;
    }

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

    public static Boolean parseValue(Serializable value) throws ParseException {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        String stringValue = String.valueOf(value);
        // check if value is 1/0 in addition to true/false
        if ("1".equals(stringValue.trim())) {
            stringValue = "true";
        }

        return Boolean.valueOf(stringValue);
    }
}
