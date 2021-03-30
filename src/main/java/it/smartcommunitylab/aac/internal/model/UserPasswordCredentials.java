package it.smartcommunitylab.aac.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.core.model.UserCredentials;

public class UserPasswordCredentials implements UserCredentials {

    private String userId;
    private String password;

    private boolean canReset = false;
    private boolean canSet = false;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
        return password;
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
