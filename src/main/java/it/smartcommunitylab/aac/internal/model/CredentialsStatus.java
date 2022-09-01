package it.smartcommunitylab.aac.internal.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CredentialsStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    REVOKED("revoked"),
    EXPIRED("expired");

    private final String value;

    CredentialsStatus(String value) {
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

    public static CredentialsStatus parse(String value) {
        for (CredentialsStatus t : CredentialsStatus.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
