package it.smartcommunitylab.aac.core.base;

import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;

public abstract class BaseIdentity implements UserIdentity, CredentialsContainer {

    protected UserAccount account;

    @Override
    public UserAccount getAccount() {
        return account;
    }

    public abstract UserAttributes getAttributes();

    public abstract Object getCredentials();

}
