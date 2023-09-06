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

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import java.util.List;
import javax.validation.constraints.NotNull;

public interface UserAccountService<U extends UserAccount> {
    public List<U> findAccountByRealm(@NotNull String realm);

    public U findAccountById(@NotNull String repository, @NotNull String id);

    public U findAccountByUuid(@NotNull String uuid);

    public List<U> findAccountByUsername(@NotNull String repository, @NotNull String username);

    public List<U> findAccountByEmail(@NotNull String repository, @NotNull String email);

    public List<U> findAccountByUser(@NotNull String repository, @NotNull String userId);

    public U addAccount(@NotNull String repository, @NotNull String id, @NotNull U reg) throws RegistrationException;

    public U updateAccount(@NotNull String repository, @NotNull String id, @NotNull U reg)
        throws NoSuchUserException, RegistrationException;

    public void deleteAccount(@NotNull String repository, @NotNull String id);
}
