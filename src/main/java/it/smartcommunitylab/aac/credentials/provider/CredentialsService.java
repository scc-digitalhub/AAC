/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.credentials.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.credentials.model.EditableUserCredentials;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import java.util.Collection;
import javax.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

/*
 * Credentials service handles credentials associated to a single user account,
 * which is handled by a given account service in the same realm
 */

public interface CredentialsService<
    R extends UserCredentials,
    E extends EditableUserCredentials,
    M extends ConfigMap,
    C extends CredentialsServiceConfig<M>
>
    extends ConfigurableResourceProvider<R, C, CredentialsServiceSettingsMap, M> {
    /*
     * (user) editable credentials
     * User can manage their credentials via a simplified (and unified) view.
     * Do note that there is *no* requirement to represent a single credential
     * consistently between editable and core.
     *
     * TODO split service in 2: core and editable
     */
    // public Collection<E> listEditableCredentials(@NotNull String userId);

    public E getEditableCredential(@NotNull String credentialId) throws NoSuchCredentialException;

    public E registerCredential(@NotNull String userId, @NotNull EditableUserCredentials credentials)
        throws RegistrationException, NoSuchUserException;

    public E editCredential(@NotNull String credentialId, @NotNull EditableUserCredentials credentials)
        throws RegistrationException, NoSuchCredentialException;

    // public void deleteEditableCredential(@NotNull String credentialId) throws NoSuchCredentialException;

    /*
     * Manage account credentials
     * Real models used for authentication purposes, exposed via services and API
     */

    public Collection<R> listCredentials(@NotNull String userId);

    public R findCredential(@NotNull String credentialId);

    public R getCredential(@NotNull String credentialId) throws NoSuchCredentialException;

    public R addCredential(@NotNull String userId, @Nullable String credentialId, @NotNull UserCredentials uc)
        throws NoSuchUserException, RegistrationException;

    public R setCredential(@NotNull String credentialId, @NotNull UserCredentials credentials)
        throws RegistrationException, NoSuchCredentialException;

    //    public void resetCredentials(String accountId, String credentialsId)
    //            throws NoSuchUserException, NoSuchCredentialException;

    public R revokeCredential(@NotNull String credentialId) throws NoSuchCredentialException, RegistrationException;

    public void deleteCredential(@NotNull String credentialId) throws NoSuchCredentialException;

    public void deleteCredentials(@NotNull String userId);

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

    public default String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }
}
