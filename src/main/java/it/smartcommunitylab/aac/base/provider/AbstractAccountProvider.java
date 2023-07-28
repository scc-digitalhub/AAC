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

package it.smartcommunitylab.aac.base.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractUserAccount;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.model.SubjectStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public abstract class AbstractAccountProvider<U extends AbstractUserAccount>
    extends AbstractProvider<U>
    implements AccountProvider<U> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    protected final UserAccountService<U> accountService;
    protected final String repositoryId;
    private ResourceEntityService resourceService;

    protected AbstractAccountProvider(
        String authority,
        String providerId,
        UserAccountService<U> accountService,
        String repositoryId,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id is mandatory");

        logger.debug(
            "create {} account provider for realm {} with id {}",
            String.valueOf(authority),
            String.valueOf(realm),
            String.valueOf(providerId)
        );

        this.accountService = accountService;
        this.repositoryId = repositoryId;
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_ACCOUNT;
    // }

    @Override
    @Transactional(readOnly = true)
    public List<U> listAccounts(String userId) {
        List<U> accounts = accountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> {
            a.setAuthority(getAuthority());
            a.setProvider(getProvider());
        });
        return accounts;
    }

    @Transactional(readOnly = true)
    public U getAccount(String accountId) throws NoSuchUserException {
        U account = findAccount(accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public U findAccount(String accountId) {
        U account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    //    @Override
    //    @Transactional(readOnly = true)
    //    public U findAccountByUuid(String uuid) {
    //        U account = accountService.findAccountByUuid(uuid);
    //        if (account == null) {
    //            return null;
    //        }
    //
    //        // check repository matches
    //        if (!repositoryId.equals(account.getRepositoryId())) {
    //            return null;
    //        }
    //
    //        // map to our authority
    //        account.setAuthority(getAuthority());
    //        account.setProvider(getProvider());
    //
    //        return account;
    //    }

    @Override
    public U lockAccount(String accountId) throws NoSuchUserException, RegistrationException {
        return updateStatus(accountId, SubjectStatus.LOCKED);
    }

    @Override
    public U unlockAccount(String accountId) throws NoSuchUserException, RegistrationException {
        return updateStatus(accountId, SubjectStatus.ACTIVE);
    }

    @Override
    public U linkAccount(String accountId, String userId) throws NoSuchUserException, RegistrationException {
        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        U account = findAccount(accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // re-link to user
        account.setUserId(userId);
        account = accountService.updateAccount(repositoryId, accountId, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public void deleteAccount(String accountId) {
        U account = findAccount(accountId);

        if (account != null) {
            // remove account
            accountService.deleteAccount(repositoryId, accountId);

            if (resourceService != null) {
                // remove resource
                resourceService.deleteResourceEntity(
                    SystemKeys.RESOURCE_ACCOUNT,
                    getAuthority(),
                    getProvider(),
                    accountId
                );
            }
        }
    }

    @Override
    public void deleteAccounts(String userId) {
        List<U> accounts = accountService.findAccountByUser(repositoryId, userId);
        for (U a : accounts) {
            // remove account
            accountService.deleteAccount(repositoryId, a.getId());

            if (resourceService != null) {
                // remove resource
                resourceService.deleteResourceEntity(
                    SystemKeys.RESOURCE_ACCOUNT,
                    getAuthority(),
                    getProvider(),
                    a.getId()
                );
            }
        }
    }

    protected U updateStatus(String accountId, SubjectStatus newStatus)
        throws NoSuchUserException, RegistrationException {
        U account = findAccount(accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus && SubjectStatus.ACTIVE != newStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        logger.debug("update account {} status from {} to {}", accountId, curStatus, newStatus);

        // update status
        account.setStatus(newStatus.getValue());
        account = accountService.updateAccount(repositoryId, accountId, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }
}
