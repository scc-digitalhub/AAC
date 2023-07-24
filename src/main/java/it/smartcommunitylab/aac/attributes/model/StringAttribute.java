package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;
import java.text.ParseException;

public class StringAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

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

    public static String parseValue(Serializable value) throws ParseException {
        if (value instanceof String) {
            return (String) value;
        }

        return String.valueOf(value);
    }
}
