package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AuthenticationScheme {

    BASIC("basic"),
    FORM("form"),
    BEARER("bearer"),
    CERTIFICATE("certificate"),
    NONE("none");

    private final String value;

    AuthenticationScheme(String value) {
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

    public static AuthenticationScheme parse(String value) {
        for (AuthenticationScheme gt : AuthenticationScheme.values()) {
            if (gt.value.equalsIgnoreCase(value)) {
                return gt;
            }
        }

        return null;
    }
}
