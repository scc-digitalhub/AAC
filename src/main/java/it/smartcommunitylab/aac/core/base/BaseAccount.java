package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.UserAccount;

/*
 * Base class for user identities
 */

public abstract class BaseAccount implements UserAccount {

    public abstract String getAuthority();

    public abstract String getRealm();

    // an identifier at authority level
    public abstract String getUserId();

    public abstract String getProvider();

    public abstract String getUsername();

}
