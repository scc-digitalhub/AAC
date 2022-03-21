package it.smartcommunitylab.aac.openid.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OIDCUserAttribute {
    SUBJECT("subject"),
    EMAIL("email"),
    USERNAME("username");

    private final String value;

    OIDCUserAttribute(String value) {
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

    public static OIDCUserAttribute parse(String value) {
        for (OIDCUserAttribute a : OIDCUserAttribute.values()) {
            if (a.value.equalsIgnoreCase(value)) {
                return a;
            }
        }

        return null;
    }
}
