package it.smartcommunitylab.aac.webauthn.model;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnCredentialCreationInfo implements Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private PublicKeyCredentialCreationOptions options;
    private String username;
    private String providerId;

    public PublicKeyCredentialCreationOptions getOptions() {
        return options;
    }

    public void setOptions(PublicKeyCredentialCreationOptions options) {
        this.options = options;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

}