package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.profiles.model.AccountProfile;

/*
 * An account used to login users into a realm, from an authority via a provider 
 */

public interface UserAccount extends UserResource, Serializable {

    // we require at least a name
    // we should make no assumptions on the content
    public String getUsername();

    // TODO remove mappers, we have extractors for profiles
    // we should implement as Converter<? UserAccount> <? Profile>
    // mapper to account profile
    public AccountProfile toProfile();

}
