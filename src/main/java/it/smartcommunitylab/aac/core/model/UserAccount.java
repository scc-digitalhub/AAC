package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * An account used to login users into a realm, from an authority via a provider 
 */

public interface UserAccount extends UserResource, Serializable {

    // we require at least a name
    // we should make no assumptions on the content
    public String getUsername();

    public String getEmailAddress();

    public boolean isEmailVerified();

    public boolean isLocked();

    default String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    // accountId is local id for provider
    public String getAccountId();

    default String getResourceId() {
        return getAccountId();
    }

}
