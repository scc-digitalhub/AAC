package it.smartcommunitylab.aac.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ScopeType {
    CLIENT("client"),
    USER("user");
//    GENERIC("generic");

    private final String value;

    ScopeType(String value) {
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

    public static ScopeType parse(String value) {
        for (ScopeType ct : ScopeType.values()) {
            if (ct.value.equalsIgnoreCase(value)) {
                return ct;
            }
        }

        return null;
    }
}
