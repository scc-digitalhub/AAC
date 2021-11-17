package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;

/*
 * An identity provider which persists some data about users
 */

public interface IdentityService extends IdentityProvider {

    /*
     * Capabilities
     */

    public boolean canRegister();

    public boolean canUpdate();

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
            String subject, UserAccount account,
            Collection<UserAttributes> attributes) throws NoSuchUserException, RegistrationException;

    public UserIdentity updateIdentity(
            String subject,
            String userId, UserAccount account,
            Collection<UserAttributes> attributes) throws NoSuchUserException, RegistrationException;

    /*
     * Registration
     */

    public String getRegistrationUrl();

}
