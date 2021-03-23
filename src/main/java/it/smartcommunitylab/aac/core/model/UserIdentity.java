package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.Collection;

import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

/*
 *  An identity, bounded to a realm, is:
 *  - managed by an authority
 *  - built by a provider 
 *  and contains
 *  - an account (from a provider)
 *  - a set of attributes (from a provider)
 *  
 *  core implementations will always match account and attributes providers
 *  i.e. attributes will be fetched from identity provider
 *  
 *  note that we do not enforce authorities to match between providers,
 *  but mixed configurations are not supported by now
 */
public interface UserIdentity extends UserResource, Serializable {

    // the login account
    public UserAccount getAccount();

    // attributes are mapped into nultiple sets
    public Collection<UserAttributes> getAttributes();

    // expose method to clear private data
    public void eraseCredentials();

    // mapper for user profiles
    // implementations need to provide this to be used for claim mapping
    public BasicProfile toBasicProfile();

    public OpenIdProfile toOpenIdProfile();
}
