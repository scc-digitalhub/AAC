package it.smartcommunitylab.aac.spid.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum SpidUserAttribute {
    SUBJECT("subject"),
    SPID_CODE("spidCode"),
    EMAIL("email"),
    USERNAME("username"),
    MOBILE_PHONE("mobilePhone"),
    FISCAL_NUMBER("fiscalNumber"),
    IVA_CODE("ivaCode");

    private final String value;

    SpidUserAttribute(String value) {
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

    public static SpidUserAttribute parse(String value) {
        for (SpidUserAttribute a : SpidUserAttribute.values()) {
            if (a.value.equalsIgnoreCase(value)) {
                return a;
            }
        }

        return null;
    }
}
