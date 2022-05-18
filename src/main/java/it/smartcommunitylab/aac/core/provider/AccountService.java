package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface AccountService<U extends UserAccount> extends AccountProvider<U> {

    /*
     * Manage accounts from this provider
     * 
     * accountId is local to provider
     * 
     * userId is globally addressable
     */
    public U registerAccount(
            String userId,
            U account) throws NoSuchUserException, RegistrationException;

    public U updateAccount(
            String accountId,
            U account) throws NoSuchUserException, RegistrationException;

    public U verifyAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U unverifyAccount(String accountId) throws NoSuchUserException, RegistrationException;

}
