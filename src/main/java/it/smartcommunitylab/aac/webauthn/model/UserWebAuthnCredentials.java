package it.smartcommunitylab.aac.webauthn.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;

@Valid
public class UserWebAuthnCredentials implements UserCredentials {

    @NotBlank
    private String userId;
    private WebAuthnCredential credential;

    private boolean canReset = false;
    private boolean canSet = false;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public WebAuthnCredential getCredential() {
        return credential;
    }

    public void setCredential(WebAuthnCredential credential) {
        this.credential = credential;
    }

    public void setCanReset(boolean canReset) {
        this.canReset = canReset;
    }

    public void setCanSet(boolean canSet) {
        this.canSet = canSet;
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return credential.toJSON();
    }

    @Override
    public boolean canSet() {
        return canSet;
    }

    @Override
    public boolean canReset() {
        return canReset;
    }

}
