package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserIdentity;

public interface IdentityService extends IdentityProvider {

    /*
     * Capabilities
     */

    public boolean canRegister();

    public boolean canUpdate();

    public boolean canDelete();

    /*
     * Services
     */

    public AccountService getAccountService();

    public CredentialsService getCredentialsService();

    /*
     * Manage identities from this provider
     * 
     * userId is globally addressable
     */

    public UserIdentity registerIdentity(
            String subject,
            Collection<Map.Entry<String, String>> attributes) throws NoSuchUserException, RegistrationException;

    public UserIdentity updateIdentity(
            String subject,
            String userId,
            Collection<Map.Entry<String, String>> attributes) throws NoSuchUserException, RegistrationException;

    public void deleteIdentity(String subjectId, String userId) throws NoSuchUserException;

    /*
     * Registration
     */

    public String getRegistrationUrl();

}
