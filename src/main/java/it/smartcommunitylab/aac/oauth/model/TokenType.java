package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

public enum TokenType {
    TOKEN_TYPE_OPAQUE("opaque"),
    TOKEN_TYPE_JWT("jwt");

    private final String value;

    TokenType(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
