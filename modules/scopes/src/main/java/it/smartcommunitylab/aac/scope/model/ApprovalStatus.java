package it.smartcommunitylab.aac.scope.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum ApprovalStatus {
    APPROVED("approved"),
    DENIED("denied");

    private final String value;

    ApprovalStatus(String value) {
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

    public static ApprovalStatus parse(String value) {
        for (ApprovalStatus m : ApprovalStatus.values()) {
            if (m.value.equalsIgnoreCase(value)) {
                return m;
            }
        }

        return null;
    }
}
