package it.smartcommunitylab.aac.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AttributeType {

    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    DATE("date"),
    DATETIME("datetime"),
    TIME("time"),
    INSTANT("instant"),
    OBJECT("object");

    private final String value;

    AttributeType(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String toString() {
        return value;
    }

    public static AttributeType parse(String value) {
        for (AttributeType ct : AttributeType.values()) {
            if (ct.value.equalsIgnoreCase(value)) {
                return ct;
            }
        }

        return null;
    }
}
