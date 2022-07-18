package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.UserCredentials;

/*
 * Credentials service handles credentials associated to a single user account
 */

public interface UserCredentialsService<C extends UserCredentials> extends ResourceProvider {

    /*
     * Capabilities
     */

//    public boolean canRead();

    public boolean canSet();

    public boolean canReset();

    public boolean canRevoke();

    /*
     * Set current credential (if only one is allowed)
     */

    public C getCredentials(String accountId) throws NoSuchUserException;

    public C setCredentials(String accountId, UserCredentials credentials) throws NoSuchUserException;

    public C resetCredentials(String accountId) throws NoSuchUserException;

    public C revokeCredentials(String accountId) throws NoSuchUserException;

    public void deleteCredentials(String accountId) throws NoSuchUserException;

    /*
     * Set specific credentials when more than one is allowed
     */
    public Collection<C> listCredentials(String accountId) throws NoSuchUserException;

    public C getCredentials(String accountId, String credentialsId) throws NoSuchUserException;

    public C setCredentials(String accountId, String credentialsId, UserCredentials credentials)
            throws NoSuchUserException;

    public C resetCredentials(String accountId, String credentialsId) throws NoSuchUserException;

    public C revokeCredentials(String accountId, String credentialsId) throws NoSuchUserException;

    public void deleteCredentials(String accountId, String credentialsId) throws NoSuchUserException;

    /*
     * Action urls
     */
    public String getSetUrl() throws NoSuchUserException;

    /*
     * At least one between resetLink or resetCredentials is required to support
     * reset. Credentials used for login should be resettable
     */
    public String getResetUrl();

}
