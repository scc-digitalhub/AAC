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

package it.smartcommunitylab.aac.identity.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.model.EditableUserAccount;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.credentials.provider.CredentialsService;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import java.util.Collection;
import org.springframework.lang.Nullable;

/*
 * Identity service are r/w repositories for local users.
 *
 * Accounts managed by services are eventually used by IdentityProviders
 */

public interface IdentityService<
    I extends UserIdentity,
    U extends UserAccount,
    E extends EditableUserAccount,
    M extends ConfigMap,
    C extends IdentityServiceConfig<M>
>
    extends ConfigurableResourceProvider<I, ConfigurableIdentityService, M, C> {
    /*
     * Services
     */

    public AccountService<U, E, ?, ?> getAccountService() throws NoSuchProviderException;

    public CredentialsService<?, ?, ?, ?> getCredentialsService(String authority) throws NoSuchProviderException;

    public Collection<CredentialsService<?, ?, ?, ?>> getCredentialsServices();

    //    public AttributeService<?, ?> getAttributeService();

    // TODO evaluate subjectResolver moved here, we manage accounts

    /*
     * Fetch identities from this provider
     */

    public I findIdentity(String userId, String identityId);

    public I getIdentity(String userId, String identityId) throws NoSuchUserException;

    public I getIdentity(String userId, String identityId, boolean loadCredentials) throws NoSuchUserException;

    public Collection<I> listIdentities(String userId);

    /*
     * Manage identities from this provider
     *
     * userId is globally addressable
     */

    public I createIdentity(@Nullable String userId, UserIdentity identity)
        throws NoSuchUserException, RegistrationException;

    public I registerIdentity(@Nullable String userId, UserIdentity identity)
        throws NoSuchUserException, RegistrationException;

    public I updateIdentity(String userId, String identityId, UserIdentity identity)
        throws NoSuchUserException, RegistrationException;

    public void deleteIdentity(String userId, String identityId) throws NoSuchUserException, RegistrationException;

    public void deleteIdentities(String userId);

    /*
     * Registration
     */

    public String getRegistrationUrl();

    //    public RegistrationProvider getRegistrationProvider();

    public default String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }
}
