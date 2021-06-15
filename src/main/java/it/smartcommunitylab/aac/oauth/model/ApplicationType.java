package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationType {
    NATIVE("native"),
    WEB("web");

    private final String value;

    ApplicationType(String value) {
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

    public static ApplicationType parse(String value) {
        for (ApplicationType t : ApplicationType.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
