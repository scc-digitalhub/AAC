package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum SubjectType {
    PUBLIC("public"),
    PAIRWISE("pairwise");

    private final String value;

    SubjectType(String value) {
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

    public static SubjectType parse(String value) {
        for (SubjectType t : SubjectType.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
