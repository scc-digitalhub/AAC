package it.smartcommunitylab.aac.core.model;

import java.util.Collection;

import org.springframework.security.core.CredentialsContainer;

public interface UserCredentialsIdentity extends UserIdentity, CredentialsContainer {

    // credentials
    public Collection<UserCredentials> getCredentials();
}
