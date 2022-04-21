package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.UserCredentials;

/*
 * Credentials service handles credentials associated to a single user account
 * 
 * credentials are *always* 1-to-1 with a user account.
 * A user may have multiple credentials within a given provider by registering multiple accounts
 */

public interface UserCredentialsService extends ResourceProvider {

    /*
     * Capabilities
     */

    public boolean canRead();

    public boolean canSet();

    public boolean canReset();

    /*
     * Implementations may not support getter/setters
     */

    public UserCredentials getCredentials(String accountId) throws NoSuchUserException;

    public UserCredentials setCredentials(String accountId, UserCredentials credentials) throws NoSuchUserException;

//    // userId is globally addressable
//    public Collection<? extends UserCredentials> listCredentials(String userId);

    public String getSetUrl() throws NoSuchUserException;

    /*
     * At least one between resetLink or resetCredentials is required to support
     * reset. Credentials used for login should be resettable
     */
    public String getResetUrl();

    public UserCredentials resetCredentials(String accountId) throws NoSuchUserException;

}
