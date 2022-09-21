package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface AccountService<U extends UserAccount> extends AccountProvider<U> {

    /*
     * Manage accounts from this provider
     * 
     * accountId is local to provider
     */
    public U createAccount(
            String accountId,
            U account) throws NoSuchUserException, RegistrationException;

    public U updateAccount(
            String accountId,
            U account) throws NoSuchUserException, RegistrationException;

    public void deleteAccount(
            String accountId) throws NoSuchUserException;

    /*
     * Account confirmation
     * 
     * verify will trigger account verification via provider
     * 
     * confirm/unconfirm directly change status
     */

    public U verifyAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U confirmAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U unconfirmAccount(String accountId) throws NoSuchUserException, RegistrationException;

}
