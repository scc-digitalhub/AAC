package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.UserCredentials;

/*
 * Credentials service handles credentials associated to a single user account,
 * which is exposed by a given identity service in the same realm
 * 
 * We currently expect a single ids per realm. 
 * TODO evaluate supporting multiple ids (from different authorities)
 */

public interface UserCredentialsService<C extends UserCredentials> extends ResourceProvider {

    /*
     * Config
     */
    public String getName();

    public String getDescription();

    // TODO expose config
    public AbstractProviderConfig getConfig();

    /*
     * Capabilities
     */

//    public boolean canRead();

//    public boolean canSet();
//
//    public boolean canReset();
//
//    public boolean canRevoke();

    /*
     * Set current credential (if only one is allowed) or all credentials
     */

    public C getCredentials(String accountId) throws NoSuchUserException;

    public C setCredentials(String accountId, UserCredentials credentials) throws NoSuchUserException;

    public void resetCredentials(String accountId) throws NoSuchUserException;

    public void revokeCredentials(String accountId) throws NoSuchUserException;

    public void deleteCredentials(String accountId) throws NoSuchUserException;

    /*
     * Set specific credentials when more than one is allowed
     */
    public Collection<C> listCredentials(String accountId) throws NoSuchUserException;

    public C getCredentials(String accountId, String credentialsId)
            throws NoSuchUserException, NoSuchCredentialException;

    public C setCredentials(String accountId, String credentialsId, UserCredentials credentials)
            throws NoSuchUserException, RegistrationException, NoSuchCredentialException;

    public void resetCredentials(String accountId, String credentialsId)
            throws NoSuchUserException, NoSuchCredentialException;

    public void revokeCredentials(String accountId, String credentialsId)
            throws NoSuchUserException, NoSuchCredentialException;

    public void deleteCredentials(String accountId, String credentialsId)
            throws NoSuchUserException, NoSuchCredentialException;

    /*
     * Action urls
     */
    public String getSetUrl() throws NoSuchUserException;

    /*
     * At least one between resetLink or resetCredentials is required to support
     * reset. Credentials used for login should be resettable, while those used for
     * MFA should be removed or revoked.
     */
    public String getResetUrl();

}
