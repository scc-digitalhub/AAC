package it.smartcommunitylab.aac.webauthn.model;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnCredentialCreationInfo {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    // TODO handle serializable, this is NOT serializable by itself
    private PublicKeyCredentialCreationOptions options;

    private String username;

    private String displayName;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

}