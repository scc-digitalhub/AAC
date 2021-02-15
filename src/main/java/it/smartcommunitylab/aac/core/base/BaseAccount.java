package it.smartcommunitylab.aac.core.base;

/*
 * Base class for user identities
 */

public abstract class BaseAccount {

    public abstract String getAuthority();

    public abstract String getRealm();

    public abstract String getUserId();

    public abstract String getProvider();

    public abstract String getUsername();

}
