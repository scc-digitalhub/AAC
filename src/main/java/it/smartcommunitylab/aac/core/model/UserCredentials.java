package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.model.Credentials;
import java.io.Serializable;
import org.springframework.security.core.CredentialsContainer;

public interface UserCredentials extends UserResource, Credentials, CredentialsContainer, Serializable {
    // credentials are associated to accounts
    public String getAccountId();

    // credentialsId is local id for provider
    public String getCredentialsId();
}
