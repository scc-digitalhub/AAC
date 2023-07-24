package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

/*
 * Response types according to
 *
 * OAuth 2.0 Multiple Response Type Encoding Practices
 * https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
 */

public enum ResponseType {
    CODE("code"),
    TOKEN("token"),
    ID_TOKEN("id_token"),
    NONE("none");

    private final String value;

    ResponseType(String value) {
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

    public static ResponseType parse(String value) {
        for (ResponseType t : ResponseType.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
