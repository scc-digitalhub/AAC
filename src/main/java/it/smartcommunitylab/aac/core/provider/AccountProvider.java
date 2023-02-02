package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface AccountProvider<U extends UserAccount> extends ResourceProvider<U> {

    /*
     * Fetch accounts from this provider
     */

//    // uuid is global
//    public U findAccountByUuid(String uuid);

    // accountId is local to provider
    public U findAccount(String accountId);

    public U getAccount(String accountId) throws NoSuchUserException;

    public void deleteAccount(String accountId) throws NoSuchUserException;

    public void deleteAccounts(String userId);

    // userId is globally addressable
    public Collection<U> listAccounts(String userId);

    /*
     * Actions on accounts
     */
    public U linkAccount(String accountId, String userId) throws NoSuchUserException, RegistrationException;

//    public UserAccount activateAccount(String accountId) throws NoSuchUserException, RegistrationException;
//
//    public UserAccount inactivateAccount(String accountId) throws NoSuchUserException, RegistrationException;

    // TODO implement lock/block via expirable locks
    public U lockAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U unlockAccount(String accountId) throws NoSuchUserException, RegistrationException;

}
