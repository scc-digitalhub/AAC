package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.profiles.model.BasicProfile;

/*
 *  An identity, bounded to a realm, is:
 *  - managed by an authority
 *  - built by a provider (which could be a composite of multiple)
 *  and contains
 *  - an account (from a provider)
 *  - a set of attributes (from a provider)
 *  
 *  implementations can mix and match providers, enforce same authority, same provider etc..
 *  core implementations will always match account and attributes providers
 *  
 */
public interface UserIdentity {

    public String getRealm();

    public String getAuthority();

    public String getProvider();

    public UserAccount getAccount();

    public UserAttributes getAttributes();

    // base attributes should be exposed where present
    public String getUsername();

    public String getEmailAddress();

    public String getFirstName();

    public String getLastName();

    public String getFullName();

    // expose method to clear private data
    public void eraseCredentials();

    // mapper for basic profile
    public BasicProfile toProfile();
}
