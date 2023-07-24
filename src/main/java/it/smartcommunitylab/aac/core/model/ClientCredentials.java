package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.model.Credentials;
import org.springframework.security.core.CredentialsContainer;

public interface ClientCredentials extends Credentials, CredentialsContainer {
    public String getClientId();

    // by default client credentials are active
    // TODO handle at implementation level
    public default boolean isActive() {
        return true;
    }

    public default boolean isExpired() {
        return false;
    }

    public default boolean isRevoked() {
        return false;
    }
}
