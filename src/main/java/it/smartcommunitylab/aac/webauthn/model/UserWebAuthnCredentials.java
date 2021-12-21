package it.smartcommunitylab.aac.webauthn.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.core.model.UserCredentials;

@Valid
public class UserWebAuthnCredentials implements UserCredentials {

    @NotBlank
    private String userId;
    private String userHandle;

    private boolean canReset = false;
    private boolean canSet = false;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
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
        return userHandle;
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
