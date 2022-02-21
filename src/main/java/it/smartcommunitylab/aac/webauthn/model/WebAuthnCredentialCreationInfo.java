package it.smartcommunitylab.aac.webauthn.model;

import java.io.Serializable;

import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

public class WebAuthnCredentialCreationInfo implements Serializable {
    private PublicKeyCredentialCreationOptions options;
    private String username;
    private String providerId;

    public PublicKeyCredentialCreationOptions getOptions() {
        return options;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setOptions(PublicKeyCredentialCreationOptions options) {
        this.options = options;
    }
}