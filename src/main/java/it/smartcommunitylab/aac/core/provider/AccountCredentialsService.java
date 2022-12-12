package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import org.springframework.lang.Nullable;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.core.model.EditableUserCredentials;
import it.smartcommunitylab.aac.core.model.UserCredentials;

/*
 * Credentials service handles credentials associated to a single user account,
 * which is handled by a given account service in the same realm 
 */

public interface AccountCredentialsService<R extends UserCredentials, E extends EditableUserCredentials, M extends ConfigMap, C extends CredentialsServiceConfig<M>>
        extends ConfigurableResourceProvider<R, ConfigurableCredentialsProvider, M, C> {

    /*
     * (user) editable credentials
     */
    public E getEditableCredential(String accountId, String credentialId) throws NoSuchCredentialException;

    public E registerCredential(String accountId, EditableUserCredentials credentials)
            throws RegistrationException, NoSuchUserException;

    public E editCredential(String accountId, String credentialId, EditableUserCredentials credentials)
            throws RegistrationException, NoSuchCredentialException;

    /*
     * Manage account credentials
     */

    public Collection<R> listCredentials(String userId);

    public R findCredential(String credentialId);

    public R getCredential(String credentialId) throws NoSuchCredentialException;

    public R addCredential(String accountId, @Nullable String credentialId, UserCredentials uc)
            throws NoSuchUserException, RegistrationException;

    public R setCredential(String accountId, String credentialId, UserCredentials credentials)
            throws RegistrationException, NoSuchCredentialException;

//    public void resetCredentials(String accountId, String credentialsId)
//            throws NoSuchUserException, NoSuchCredentialException;

    public R revokeCredential(String credentialId) throws NoSuchCredentialException, RegistrationException;

    public void deleteCredential(String credentialId) throws NoSuchCredentialException;

    public void deleteCredentials(String userId);

//    /*
//     * Action urls
//     */
//    public String getSetUrl() throws NoSuchUserException;
    public String getRegisterUrl();

    public String getEditUrl(String credentialsId) throws NoSuchCredentialException;

//
//    /*
//     * At least one between resetLink or resetCredentials is required to support
//     * reset. Credentials used for login should be resettable, while those used for
//     * MFA should be removed or revoked.
//     */
//    public String getResetUrl();

    default public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

}
