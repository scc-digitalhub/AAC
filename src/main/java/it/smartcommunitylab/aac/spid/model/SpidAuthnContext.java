package it.smartcommunitylab.aac.spid.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SpidAuthnContext {
    SPID_L1("https://www.spid.gov.it/SpidL1"),
    SPID_L2("https://www.spid.gov.it/SpidL2"),
    SPID_L3("https://www.spid.gov.it/SpidL3");

    private final String value;

    SpidAuthnContext(String value) {
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

    public static SpidAuthnContext parse(String value) {
        for (SpidAuthnContext t : SpidAuthnContext.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
