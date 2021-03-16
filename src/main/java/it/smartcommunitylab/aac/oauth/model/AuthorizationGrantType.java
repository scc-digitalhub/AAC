package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

public enum AuthorizationGrantType {

    AUTHORIZATION_CODE("authorization_code"),
    IMPLICIT("implicit"),
    REFRESH_TOKEN("refresh_token"),
    CLIENT_CREDENTIALS("client_credentials"),
    PASSWORD("password"),
    DEVICE_CODE(
            "urn:ietf:params:oauth:grant-type:device_code");

    private final String value;

    AuthorizationGrantType(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
