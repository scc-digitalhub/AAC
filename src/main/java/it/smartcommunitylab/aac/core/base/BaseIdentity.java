package it.smartcommunitylab.aac.core.base;

import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * Base class for user identities
 * implements erase credentials if account is a credentialsContainer
 */
public abstract class BaseIdentity extends AbstractIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public BaseIdentity(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    public void eraseCredentials() {
        if (getAccount() instanceof CredentialsContainer) {
            ((CredentialsContainer) getAccount()).eraseCredentials();
        }
    }

}
