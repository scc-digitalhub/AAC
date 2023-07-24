package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum AuthorizationGrantType {
    AUTHORIZATION_CODE("authorization_code"),
    IMPLICIT("implicit"),
    REFRESH_TOKEN("refresh_token"),
    CLIENT_CREDENTIALS("client_credentials"),
    PASSWORD("password"),
    DEVICE_CODE("urn:ietf:params:oauth:grant-type:device_code");

    private final String value;

    AuthorizationGrantType(String value) {
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

    public static AuthorizationGrantType parse(String value) {
        for (AuthorizationGrantType gt : AuthorizationGrantType.values()) {
            if (gt.value.equalsIgnoreCase(value)) {
                return gt;
            }
        }

        return null;
    }
}
