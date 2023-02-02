package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.Collection;

import it.smartcommunitylab.aac.SystemKeys;

/*
 *  An identity, bounded to a realm, is:
 *  - managed by an authority
 *  - built by a provider 
 *  and contains
 *  - an account (from a provider)
 *  - a set of attributes (from a provider)
 *  - an authenticated principal when user performed auth via this provider
 *  
 *  core implementations will always match account and attributes providers
 *  i.e. attributes will be fetched from identity provider
 *  
 *  do note that identities *may* contain credentials in accounts OR principal.
 */
public interface UserIdentity extends UserResource, Serializable {

    // authenticated principal (if available)
    public UserAuthenticatedPrincipal getPrincipal();

    // the account
    public UserAccount getAccount();

    // attributes are mapped into multiple sets
    public Collection<UserAttributes> getAttributes();

    default String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    // id is global
    // by default user identity id is the account id
    // the same id should be assigned to authenticatedPrincipal
    default String getId() {
        return getAccount() == null ? null : getAccount().getId();
    }

    // uuid is global
    // by default user identity uuid is the account uuid
    // the same id should be assigned to authenticatedPrincipal
    default String getUuid() {
        return getAccount() == null ? null : getAccount().getUuid();
    }

    // resourceId is local
    // by default user identity id is the account id
    default String getIdentityId() {
        return getAccount() == null ? null : getAccount().getAccountId();
    }

}
