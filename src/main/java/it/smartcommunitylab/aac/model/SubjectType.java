package it.smartcommunitylab.aac.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SubjectType {

    USER("user"),
    CLIENT("client"),
    SERVICE("service");

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
        for (SubjectType ct : SubjectType.values()) {
            if (ct.value.equalsIgnoreCase(value)) {
                return ct;
            }
        }

        return null;
    }
}
