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

package it.smartcommunitylab.aac.credentials.persistence;

import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;

public interface UserCredentialsService<C extends UserCredentials> {
    public Collection<C> findCredentials(@NotNull String repositoryId);

    public Collection<C> findCredentialsByRealm(@NotNull String realm);

    public C findCredentialsById(@NotNull String repository, @NotNull String id);

    public C findCredentialsByUuid(@NotNull String uuid);

    //TODO remove, credentials are associated to USER not ACCOUNT
    // public Collection<C> findCredentialsByAccount(@NotNull String repository, @NotNull String accountId);

    public Collection<C> findCredentialsByUser(@NotNull String repository, @NotNull String userId);

    public C addCredentials(@NotNull String repository, @NotNull String id, @NotNull C reg)
        throws RegistrationException;

    public C updateCredentials(@NotNull String repository, @NotNull String id, @NotNull C reg)
        throws NoSuchCredentialException, RegistrationException;

    public void deleteCredentials(@NotNull String repository, @NotNull String id);

    public void deleteAllCredentials(@NotNull String repository, @NotNull Collection<String> id);

    public void deleteAllCredentialsByUser(@NotNull String repository, @NotNull String userId);
    //TODO remove, credentials are associated to USER not ACCOUNT
    // public void deleteAllCredentialsByAccount(@NotNull String repository, @NotNull String account);
}
