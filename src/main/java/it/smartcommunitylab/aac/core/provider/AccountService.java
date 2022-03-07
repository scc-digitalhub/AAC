package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface AccountService extends AccountProvider {

    /*
     * Capabilities
     */

    public boolean canRegister();

    public boolean canUpdate();

    public boolean canVerify();

    /*
     * Manage accounts from this provider
     * 
     * userId is globally addressable
     */

    // TODO rewrite with generics, needs userAccount as abstract class or
    // workarounds
    public UserAccount registerAccount(
            String userId,
            UserAccount account) throws NoSuchUserException, RegistrationException;

    public UserAccount updateAccount(
            String accountId,
            UserAccount account) throws NoSuchUserException, RegistrationException;

    public UserAccount verifyAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public UserAccount unverifyAccount(String accountId) throws NoSuchUserException, RegistrationException;

}
