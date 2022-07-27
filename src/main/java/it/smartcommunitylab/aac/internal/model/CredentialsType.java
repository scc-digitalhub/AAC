package it.smartcommunitylab.aac.internal.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CredentialsType {
    PASSWORD("password"),
//    LINK("link"),
    NONE("none");

    private final String value;

    CredentialsType(String value) {
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

    public static CredentialsType parse(String value) {
        for (CredentialsType t : CredentialsType.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
