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

    public boolean canDelete();

    /*
     * Services
     */

    public AccountService getAccountService();

    public CredentialsService getCredentialsService();

    /*
     * fetch identities from this provider
     * 
     * implementations are not required to support this
     */

    // userId is provider-specific
    public UserIdentity getIdentity(String subject, String userId) throws NoSuchUserException;

    public UserIdentity getIdentity(String subject, String userId, boolean fetchAttributes) throws NoSuchUserException;

    /*
     * fetch for subject
     * 
     * opt-in, loads identities outside login for persisted accounts linked to the
     * subject
     * 
     * providers implementing this will enable the managers to fetch identities
     * outside the login flow!
     */

    public Collection<? extends UserIdentity> listIdentities(String subject);

    public Collection<? extends UserIdentity> listIdentities(String subject, boolean fetchAttributes);

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

    public void deleteIdentity(String subjectId, String userId) throws NoSuchUserException;

    public void deleteIdentities(String subjectId);

    /*
     * Registration
     */

    public String getRegistrationUrl();

}
