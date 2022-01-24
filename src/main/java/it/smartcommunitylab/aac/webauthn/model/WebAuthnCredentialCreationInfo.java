package it.smartcommunitylab.aac.webauthn.model;

import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

public class WebAuthnCredentialCreationInfo {
    public PublicKeyCredentialCreationOptions options;
    public String username;
    public String realm;
    public String providerId;
}