package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum ApplicationType {
    NATIVE("native"),
    WEB("web"),
    MACHINE("machine"),
    SPA("spa"),
    INTROSPECTION("introspection");

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
