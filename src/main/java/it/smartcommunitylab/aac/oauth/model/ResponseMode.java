package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

/*
 * Response modes according to
 *
 * OAuth 2.0 Multiple Response Type Encoding Practices
 * https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
 *
 * OAuth 2.0 Form Post Response Mode
 * https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html
 */

public enum ResponseMode {
    QUERY("query"),
    FRAGMENT("fragment"),
    FORM_POST("form_post");

    private final String value;

    ResponseMode(String value) {
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

    public static ResponseMode parse(String value) {
        for (ResponseMode t : ResponseMode.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
