package it.smartcommunitylab.aac.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum SubjectStatus {
    ACTIVE("active"),
    LOCKED("locked"),
    BLOCKED("blocked"),
    INACTIVE("inactive");

    private final String value;

    SubjectStatus(String value) {
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

    public static SubjectStatus parse(String value) {
        for (SubjectStatus st : SubjectStatus.values()) {
            if (st.value.equalsIgnoreCase(value)) {
                return st;
            }
        }

        return null;
    }
}
