package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface AccountService extends ResourceProvider {
    
    /*
     * Capabilities
     */

    public boolean canRegister();

    public boolean canUpdate();

    public boolean canDelete();
    
    /*
     * Manage accounts from this provider
     * 
     * userId is globally addressable
     */

    public UserAccount registerAccount(
            String subject,
            Collection<Map.Entry<String, String>> attributes) throws NoSuchUserException, RegistrationException;

    public UserAccount updateAccount(
            String subject,
            String userId,
            Collection<Map.Entry<String, String>> attributes) throws NoSuchUserException, RegistrationException;

    public void deleteAccount(String subjectId, String userId) throws NoSuchUserException;
}
