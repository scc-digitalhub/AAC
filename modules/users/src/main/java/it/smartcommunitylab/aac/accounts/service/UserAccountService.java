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

package it.smartcommunitylab.aac.accounts.service;

import it.smartcommunitylab.aac.accounts.model.EditableUserAccount;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.accounts.provider.AccountProviderConfig;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.model.ResourceContext;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserAccountService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserEntityService userService;

    @Autowired
    private AccountProviderService accountProviderService;

    /*
     * User account via key
     */
    @Transactional(readOnly = false)
    public UserAccount findUserAccount(String realm, String key)
        throws NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("find user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String provider = res.getProvider();
        String id = res.getId();
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(id)) {
            return null;
        }

        // fetch service
        AccountProvider<?, ?, ?> service = accountProviderService.getAccountProvider(provider);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // find account
        return service.findAccount(id);
    }

    @Transactional(readOnly = false)
    public UserAccount getUserAccount(String realm, String key)
        throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("get user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authority = res.getAuthority();
        String provider = res.getProvider();
        String id = res.getId();
        if (!StringUtils.hasText(authority) || !StringUtils.hasText(provider) || !StringUtils.hasText(id)) {
            return null;
        }

        logger.debug("get user account {} via provider {}:{}", id, authority, provider);

        // fetch service
        AccountProvider<?, ?, ?> service = accountProviderService.getAccountProvider(provider);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // fetch account
        return service.getAccount(id);
    }

    @Transactional(readOnly = false)
    public EditableUserAccount getEditableUserAccount(String realm, String key)
        throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("get editable user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authority = res.getAuthority();
        String provider = res.getProvider();
        String id = res.getId();
        if (!StringUtils.hasText(authority) || !StringUtils.hasText(provider) || !StringUtils.hasText(id)) {
            return null;
        }

        logger.debug("get editable user account {} via provider {}:{}", id, authority, provider);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(provider);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // fetch account
        UserAccount account = service.getAccount(id);

        // fetch editable
        return ((AccountService<?, ?, ?, ?>) service).getEditableAccount(account.getUserId(), id);
    }

    @Transactional(readOnly = false)
    public Collection<EditableUserAccount> listEditableUserAccounts(String realm, String userId)
        throws NoSuchUserException {
        logger.debug("get editable user {} accounts", StringUtils.trimAllWhitespace(userId));

        // collect from all providers for the same realm
        Collection<
            AccountService<
                ? extends UserAccount,
                ? extends EditableUserAccount,
                ConfigMap,
                AccountProviderConfig<ConfigMap>
            >
        > services = accountProviderService.listAccountServicesByRealm(realm);

        List<EditableUserAccount> accounts = services
            .stream()
            .flatMap(s ->
                s
                    .listAccounts(userId)
                    .stream()
                    .map(a -> {
                        try {
                            return s.getEditableAccount(a.getUserId(), a.getAccountId());
                        } catch (NoSuchUserException e1) {
                            return null;
                        }
                    })
                    .filter(a -> a != null))
            .collect(Collectors.toList());

        return accounts;
    }

    @Transactional(readOnly = false)
    public Collection<UserAccount> listUserAccounts(String realm, String userId) throws NoSuchUserException {
        logger.debug("get user {} accounts", StringUtils.trimAllWhitespace(userId));

        UserEntity ue = userService.getUser(userId);
        if (!ue.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // collect from all providers for the same realm
        Collection<AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>>> services =
            accountProviderService.listAccountProvidersByRealm(realm);

        List<UserAccount> accounts = services
            .stream()
            .flatMap(s -> s.listAccounts(userId).stream())
            .collect(Collectors.toList());

        return accounts;
    }

    /*
     * User account via providers
     */
    @Transactional(readOnly = false)
    public EditableUserAccount registerUserAccount(
        String realm,
        String authority,
        String providerId,
        @Nullable String userId,
        EditableUserAccount reg
    ) throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug(
            "register user {} account via provider {}",
            StringUtils.trimAllWhitespace(String.valueOf(userId)),
            StringUtils.trimAllWhitespace(providerId)
        );

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // execute
        return ((AccountService<?, ?, ?, ?>) service).registerAccount(userId, reg);
    }

    @Transactional(readOnly = false)
    public EditableUserAccount editUserAccount(String realm, String key, EditableUserAccount reg)
        throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("edit user account {}", StringUtils.trimAllWhitespace(key));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getId();
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId) || !StringUtils.hasText(accountId)) {
            return null;
        }

        logger.debug("edit user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // find account
        UserAccount account = service.getAccount(accountId);

        // execute
        return ((AccountService<?, ?, ?, ?>) service).editAccount(account.getUserId(), accountId, reg);
    }

    @Transactional(readOnly = false)
    public UserAccount createUserAccount(
        String realm,
        String authority,
        String providerId,
        String userId,
        @Nullable String accountId,
        UserAccount reg
    ) throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug(
            "create user {} account {} via provider {}",
            StringUtils.trimAllWhitespace(String.valueOf(userId)),
            StringUtils.trimAllWhitespace(String.valueOf(accountId)),
            StringUtils.trimAllWhitespace(providerId)
        );

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // execute
        return ((AccountService<?, ?, ?, ?>) service).createAccount(userId, accountId, reg);
    }

    @Transactional(readOnly = false)
    public UserAccount updateUserAccount(String realm, String key, UserAccount reg)
        throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("update user account {}", StringUtils.trimAllWhitespace(key));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getId();
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId) || !StringUtils.hasText(accountId)) {
            return null;
        }

        logger.debug("update user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountProvider<?, ?, ?> service = accountProviderService.getAccountProvider(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // find account
        UserAccount ua = service.getAccount(accountId);
        String userId = ua.getUserId();

        if (service instanceof AccountService) {
            // execute
            return ((AccountService<?, ?, ?, ?>) service).updateAccount(userId, accountId, reg);
        }

        throw new NoSuchProviderException();
    }

    @Transactional(readOnly = false)
    public UserAccount verifyUserAccount(String realm, String key)
        throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("verify user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getId();
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId) || !StringUtils.hasText(accountId)) {
            return null;
        }

        logger.debug("verify user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // execute
        return service.verifyAccount(accountId);
    }

    @Transactional(readOnly = false)
    public UserAccount confirmUserAccount(String realm, String key)
        throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("confirm user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getId();
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId) || !StringUtils.hasText(accountId)) {
            return null;
        }

        logger.debug("confirm user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // execute
        return service.confirmAccount(accountId);
    }

    @Transactional(readOnly = false)
    public UserAccount unconfirmUserAccount(String realm, String key)
        throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("unconfirm user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getId();
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId) || !StringUtils.hasText(accountId)) {
            return null;
        }

        logger.debug("unconfirm user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // execute
        return service.unconfirmAccount(accountId);
    }

    @Transactional(readOnly = false)
    public void deleteUserAccount(String realm, String key)
        throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("delete user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getId();
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId) || !StringUtils.hasText(accountId)) {
            throw new RegistrationException("invalid key");
        }

        logger.debug("delete user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // TODO delete via idp provider to also clear attributes
        service.deleteAccount(accountId);
    }

    @Transactional(readOnly = false)
    public void deleteAllUserAccouts(String realm, String userId)
        throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("delete all user {} accounts", StringUtils.trimAllWhitespace(userId));

        // fetch user
        UserEntity ue = userService.findUser(userId);
        if (ue != null && !ue.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // collect from all providers for the same realm
        Collection<AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>>> services =
            accountProviderService.listAccountProvidersByRealm(realm);

        services.forEach(s -> s.deleteAccounts(userId));
    }

    @Transactional(readOnly = false)
    public UserAccount lockUserAccount(String realm, String key)
        throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("lock user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getId();
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId) || !StringUtils.hasText(accountId)) {
            throw new RegistrationException("invalid key");
        }

        logger.debug("delete user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // lock account to disable login
        return service.lockAccount(accountId);
    }

    @Transactional(readOnly = false)
    public UserAccount unlockUserAccount(String realm, String key)
        throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("unlock user account {}", StringUtils.trimAllWhitespace(key));

        // resolve resource
        Resource res = ResourceContext.resolveKey(key);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getId();
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId) || !StringUtils.hasText(accountId)) {
            throw new RegistrationException("invalid key");
        }

        logger.debug("delete user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountProviderService.getAccountService(providerId);
        if (!service.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // unlock account to enable login
        return service.unlockAccount(accountId);
    }
}
