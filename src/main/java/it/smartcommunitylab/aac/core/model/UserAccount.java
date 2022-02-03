package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

/*
 * An account used to login users into a realm, from an authority via a provider 
 */

public interface UserAccount extends UserResource, Serializable {

    // we require at least a name
    // we should make no assumptions on the content
    public String getUsername();

    public String getEmailAddress();

    public boolean isEmailVerified();

}
