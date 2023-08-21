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
import it.smartcommunitylab.aac.accounts.base.AbstractAccountProvider;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class InternalAccountProvider extends AbstractAccountProvider<InternalUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InternalAccountProvider(
        String providerId,
        UserAccountService<InternalUserAccount> accountService,
        String repositoryId,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_INTERNAL, providerId, accountService, repositoryId, realm);
    }

    public InternalAccountProvider(
        String authority,
        String providerId,
        UserAccountService<InternalUserAccount> accountService,
        String repositoryId,
        String realm
    ) {
        super(authority, providerId, accountService, repositoryId, realm);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUsername(String username) {
        return findAccount(username);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByEmail(String email) {
        logger.debug("find account for email {}", String.valueOf(email));

        // we pick first account matching email, repository should contain unique
        // email+provider
        InternalUserAccount account = accountService
            .findAccountByEmail(repositoryId, email)
            .stream()
            .filter(a -> a.isEmailVerified())
            .findFirst()
            .orElse(null);

        if (logger.isTraceEnabled()) {
            logger.trace("account: {}", String.valueOf(account));
        }

        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }
}
