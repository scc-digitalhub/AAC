package it.smartcommunitylab.aac.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum PersistenceMode {
    NONE("none"),
    //    MEMORY("memory"),
    REPOSITORY("repository"),
    SESSION("session");

    private final String value;

    PersistenceMode(String value) {
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

    public static PersistenceMode parse(String value) {
        for (PersistenceMode pm : PersistenceMode.values()) {
            if (pm.value.equalsIgnoreCase(value)) {
                return pm;
            }
        }

        return null;
    }
}
