package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.model.Credentials;

public interface UserCredentials extends UserResource, Credentials, CredentialsContainer, Serializable {

    // credentials are associated to accounts
    public String getAccountId();

    public boolean isChangeOnFirstAccess();

    // credentialsId is local id for provider
    public String getCredentialsId();

    default String getResourceId() {
        return getCredentialsId();
    }
}
