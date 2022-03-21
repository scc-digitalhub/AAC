package it.smartcommunitylab.aac.saml.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SamlUserAttribute {
    SUBJECT("subject"),
    EMAIL("email"),
    USERNAME("username");

    private final String value;

    SamlUserAttribute(String value) {
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

    public static SamlUserAttribute parse(String value) {
        for (SamlUserAttribute a : SamlUserAttribute.values()) {
            if (a.value.equalsIgnoreCase(value)) {
                return a;
            }
        }

        return null;
    }
}
