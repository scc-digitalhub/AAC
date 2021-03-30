package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.UserCredentials;

public interface CredentialsService extends ResourceProvider {

    /*
     * Capabilities
     */

    public boolean canRead();

    public boolean canSet();

    public boolean canReset();

    /*
     * Implementations may not support getter/setters
     */

    public UserCredentials getUserCredentials(String userId) throws NoSuchUserException;

    public UserCredentials setUserCredentials(String userId, UserCredentials credentials) throws NoSuchUserException;

    /*
     * At least one between resetLink or resetCredentials is required to support
     * reset. Credentials used for login should be resettable
     */
    public String getResetUrl(String userId) throws NoSuchUserException;

    public UserCredentials resetUserCredentials(String userId) throws NoSuchUserException;

}
