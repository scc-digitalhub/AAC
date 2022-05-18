package it.smartcommunitylab.aac.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserStatus {

    ACTIVE("active"),
    LOCKED("locked"),
    BLOCKED("blocked"),
    INACTIVE("inactive");

    private final String value;

    UserStatus(String value) {
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

    public static UserStatus parse(String value) {
        for (UserStatus st : UserStatus.values()) {
            if (st.value.equalsIgnoreCase(value)) {
                return st;
            }
        }

        return null;
    }
}
