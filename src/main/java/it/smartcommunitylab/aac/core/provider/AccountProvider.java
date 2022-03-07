package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface AccountProvider extends ResourceProvider {

    /*
     * Fetch accounts from this provider
     */

    // accountId is local to provider
    public UserAccount getAccount(String accountId) throws NoSuchUserException;

    public void deleteAccount(String accountId) throws NoSuchUserException;

    // userId is globally addressable
    public Collection<? extends UserAccount> listAccounts(String userId);

    /*
     * Actions on accounts
     */
    public UserAccount lockAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public UserAccount unlockAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public UserAccount blockAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public UserAccount unblockAccount(String accountId) throws NoSuchUserException, RegistrationException;

}
