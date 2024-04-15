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

package it.smartcommunitylab.aac.accounts.provider;

import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.users.provider.UserPersistedResourceProvider;
import java.util.Collection;

//TODO split identityAccountProvider out, make this Configurable (merge from accountService)
public interface AccountProvider<U extends UserAccount> extends UserPersistedResourceProvider<U> {
    /*
     * Fetch accounts from this provider
     */

    //    // uuid is global
    //    public U findAccountByUuid(String uuid);

    // accountId is local to provider
    public U findAccount(String accountId);

    public U getAccount(String accountId) throws NoSuchUserException;

    public void deleteAccount(String accountId);

    public void deleteAccounts(String userId);

    // userId is globally addressable
    public Collection<U> listAccounts(String userId);

    public Collection<U> listAccounts();

    /*
     * Actions on accounts
     */
    public U linkAccount(String accountId, String userId) throws NoSuchUserException, RegistrationException;

    //    public UserAccount activateAccount(String accountId) throws NoSuchUserException, RegistrationException;
    //
    //    public UserAccount inactivateAccount(String accountId) throws NoSuchUserException, RegistrationException;

    // TODO implement lock/block via expirable locks
    public U lockAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U unlockAccount(String accountId) throws NoSuchUserException, RegistrationException;

    /*
     * As Resources
     */

    @Override
    default Collection<U> listResources() {
        return listAccounts();
    }

    @Override
    default Collection<U> listResourcesByUser(String userId) {
        return listAccounts(userId);
    }

    @Override
    default U findResource(String id) {
        return findAccount(id);
    }

    @Override
    default U getResource(String id) throws NoSuchResourceException {
        return getAccount(id);
    }

    @Override
    default void deleteResource(String id) {
        deleteAccount(id);
    }

    @Override
    default void deleteResourcesByUser(String userId) {
        deleteAccounts(userId);
    }
}
