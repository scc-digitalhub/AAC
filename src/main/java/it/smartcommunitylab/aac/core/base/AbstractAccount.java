package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;

/*
 * Abstract class for user accounts
 * 
 * all implementations should derive from this
 */

public abstract class AbstractAccount extends AbstractBaseResource implements UserAccount {

    protected AbstractAccount(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

}
