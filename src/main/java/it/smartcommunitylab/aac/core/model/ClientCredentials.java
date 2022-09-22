package it.smartcommunitylab.aac.core.model;

import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.model.Credentials;

public interface ClientCredentials extends Credentials, CredentialsContainer {

    public String getClientId();

    // by default client credentials are active
    // TODO handle at implementation level
    default public boolean isActive() {
        return true;
    }

    default public boolean isExpired() {
        return false;
    }

    default public boolean isRevoked() {
        return false;
    }

}
