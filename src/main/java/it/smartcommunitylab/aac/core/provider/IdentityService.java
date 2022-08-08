package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;

/*
 * Identity service are r/w repositories for local users.
 * 
 * Accounts managed by services are eventually used by IdentityProviders
 */

public interface IdentityService<I extends UserIdentity, U extends UserAccount>
        extends IdentityProvider<I> {

//    /*
//     * Config
//     */
//    public String getName();
//
//    public String getDescription();
//
//    // TODO expose config
//    public AbstractProviderConfig getConfig();

    /*
     * Services
     */

    public AccountService<U> getAccountService();

    /*
     * Manage identities from this provider
     * 
     * userId is globally addressable
     */
    public I createIdentity(
            String userId, UserIdentity identity) throws NoSuchUserException, RegistrationException;

    public I registerIdentity(
            String userId, UserIdentity identity)
            throws NoSuchUserException, RegistrationException;

    public I updateIdentity(
            String userId,
            String identityId, UserIdentity identity) throws NoSuchUserException, RegistrationException;

//    public void deleteIdentity(
//            String userId,
//            String identityId) throws NoSuchUserException, RegistrationException;
    /*
     * Registration
     */

    public String getRegistrationUrl();

}
