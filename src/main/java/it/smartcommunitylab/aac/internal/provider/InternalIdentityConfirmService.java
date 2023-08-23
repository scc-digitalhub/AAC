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

package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public class InternalIdentityConfirmService extends AbstractProvider<UserCredentials> {

    private final InternalUserConfirmKeyService confirmKeyService;
    private final UserAccountService<InternalUserAccount> accountService;
    private final String repositoryId;

    public InternalIdentityConfirmService(
        String providerId,
        UserAccountService<InternalUserAccount> accountService,
        InternalUserConfirmKeyService confirmKeyService,
        InternalIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm service is mandatory");
        Assert.notNull(config, "config is mandatory");

        this.accountService = accountService;
        this.confirmKeyService = confirmKeyService;

        // repositoryId from config
        this.repositoryId = config.getRepositoryId();
    }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_CREDENTIALS;
    // }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByConfirmationKey(String key) {
        InternalUserAccount account = confirmKeyService.findAccountByConfirmationKey(repositoryId, key);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    public InternalUserAccount confirmAccount(String username, String key)
        throws NoSuchUserException, RegistrationException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            return null;
        }

        account = confirmKeyService.confirmAccount(repositoryId, username, key);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }
}
