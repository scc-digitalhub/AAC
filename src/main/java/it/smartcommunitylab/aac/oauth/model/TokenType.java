package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TokenType {
    TOKEN_TYPE_OPAQUE("opaque"),
    TOKEN_TYPE_JWT("jwt");

    private final String value;

    TokenType(String value) {
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

    public static TokenType parse(String value) {
        for (TokenType t : TokenType.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
