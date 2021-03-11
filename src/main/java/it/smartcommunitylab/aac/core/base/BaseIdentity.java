package it.smartcommunitylab.aac.core.base;

import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.profiles.model.BasicProfile;

/*
 * Base class for user identities
 * implements erase credentials if account is a credentialsContainer
 */
public abstract class BaseIdentity extends AbstractIdentity {

    public BaseIdentity(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    public void eraseCredentials() {
        if (getAccount() instanceof CredentialsContainer) {
            ((CredentialsContainer) getAccount()).eraseCredentials();
        }
    }
    


}
