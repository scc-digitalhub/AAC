package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.Collection;

import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;

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

    // login principal
    public UserAuthenticatedPrincipal getPrincipal();

    // the account
    public UserAccount getAccount();

    // attributes are mapped into multiple sets
    public Collection<UserAttributes> getAttributes();

    // expose method to clear private data
    public void eraseCredentials();

}
