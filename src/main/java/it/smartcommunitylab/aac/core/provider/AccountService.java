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

    /*
     * Manage accounts from this provider
     * 
     * userId is globally addressable
     */

    // TODO rewrite with generics, needs userAccount as abstract class or
    // workarounds
    public UserAccount registerAccount(
            String subject,
            UserAccount account) throws NoSuchUserException, RegistrationException;

    public UserAccount updateAccount(
            String userId,
            UserAccount account) throws NoSuchUserException, RegistrationException;

}
