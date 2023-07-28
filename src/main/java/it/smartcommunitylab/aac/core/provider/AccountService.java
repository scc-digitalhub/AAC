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

package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.config.AccountServiceConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAccountProvider;
import org.springframework.lang.Nullable;

//TODO split editable into own service
public interface AccountService<
    U extends UserAccount, E extends EditableUserAccount, M extends ConfigMap, C extends AccountServiceConfig<M>
>
    extends ConfigurableResourceProvider<U, ConfigurableAccountProvider, M, C>, AccountProvider<U> {
    /*
     * Editable accounts from this provider
     *
     * accountId is local to provider.
     * Editable account are user-editable
     */
    public E getEditableAccount(String userId, String accountId) throws NoSuchUserException;

    public E registerAccount(@Nullable String userId, EditableUserAccount account)
        throws NoSuchUserException, RegistrationException;

    public E editAccount(String userId, String accountId, EditableUserAccount account)
        throws NoSuchUserException, RegistrationException;

    /*
     * Manage accounts from this provider
     *
     * accountId is local to provider.
     * Editable account are user-editable
     */
    public U createAccount(@Nullable String userId, @Nullable String accountId, UserAccount account)
        throws NoSuchUserException, RegistrationException;

    public U updateAccount(String userId, String accountId, UserAccount account)
        throws NoSuchUserException, RegistrationException;

    /*
     * Account confirmation
     *
     * verify will trigger account verification via provider
     *
     * confirm/unconfirm directly change status
     */

    public U verifyAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U confirmAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U unconfirmAccount(String accountId) throws NoSuchUserException, RegistrationException;

    /*
     * Registration (for editable)
     */

    public String getRegistrationUrl();

    //    public RegistrationProvider getRegistrationProvider();

    public default String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }
}
