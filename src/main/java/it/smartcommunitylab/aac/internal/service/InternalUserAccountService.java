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

package it.smartcommunitylab.aac.internal.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountId;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.model.Subject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/*
 * An internal service which handles persistence for internal user accounts, via JPA
 *
 * We enforce detach on fetch to keep internal datasource isolated.
 */

@Transactional
public class InternalUserAccountService
    implements UserAccountService<InternalUserAccount>, InternalUserConfirmKeyService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalUserAccountRepository accountRepository;
    private final SubjectService subjectService;

    public InternalUserAccountService(InternalUserAccountRepository accountRepository, SubjectService subjectService) {
        Assert.notNull(accountRepository, "account repository is required");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.accountRepository = accountRepository;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findAccountByRealm(String realm) {
        logger.debug("find account for realm {}", String.valueOf(realm));

        List<InternalUserAccount> accounts = accountRepository.findByRealm(realm);
        return accounts
            .stream()
            .map(a -> {
                return accountRepository.detach(a);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountById(String repository, String username) {
        logger.debug(
            "find account with username {} in repository {}",
            String.valueOf(username),
            String.valueOf(repository)
        );

        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(repository, username));
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findAccountByUsername(String repository, String username) {
        logger.debug(
            "find account with username {} in repository {}",
            String.valueOf(username),
            String.valueOf(repository)
        );

        // we have at most 1 account with a given username, since username == id
        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(repository, username));
        if (account == null) {
            return Collections.emptyList();
        }

        account = accountRepository.detach(account);

        return Collections.singletonList(account);
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findAccountByEmail(String repository, String email) {
        logger.debug("find account with email {} in repository {}", String.valueOf(email), String.valueOf(repository));

        List<InternalUserAccount> accounts = accountRepository.findByRepositoryIdAndEmail(repository, email);
        return accounts
            .stream()
            .map(a -> {
                return accountRepository.detach(a);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByConfirmationKey(String repository, String key) {
        logger.debug(
            "find account with confirmation key {} in repository {}",
            String.valueOf(key),
            String.valueOf(repository)
        );

        InternalUserAccount account = accountRepository.findByRepositoryIdAndConfirmationKey(repository, key);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUuid(String uuid) {
        logger.debug("find account with uuid {}", String.valueOf(uuid));

        InternalUserAccount account = accountRepository.findByUuid(uuid);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findAccountByUser(String repository, String userId) {
        logger.debug("find account for user {} in repository {}", String.valueOf(userId), String.valueOf(repository));

        List<InternalUserAccount> accounts = accountRepository.findByUserIdAndRepositoryId(userId, repository);
        return accounts
            .stream()
            .map(a -> {
                return accountRepository.detach(a);
            })
            .collect(Collectors.toList());
    }

    /*
     * CRUD
     */
    public InternalUserAccount addAccount(String repository, String username, InternalUserAccount reg)
        throws RegistrationException {
        logger.debug(
            "add account with username {} in repository {}",
            String.valueOf(username),
            String.valueOf(repository)
        );

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        try {
            InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(repository, username));
            if (account != null) {
                throw new DuplicatedDataException("username");
            }

            // create subject when needed
            String uuid = reg.getUuid();
            if (!StringUtils.hasText(uuid)) {
                uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
            }

            Subject s = subjectService.findSubject(uuid);
            if (s == null) {
                logger.debug("create new subject for username {}", String.valueOf(username));
                s = subjectService.addSubject(uuid, reg.getRealm(), SystemKeys.RESOURCE_ACCOUNT, username);
            } else {
                if (
                    !s.getRealm().equals(reg.getRealm()) ||
                    !SystemKeys.RESOURCE_ACCOUNT.equals(s.getType()) ||
                    !username.equals(s.getSubjectId())
                ) {
                    throw new RegistrationException("subject-mismatch");
                }
            }

            // we explode model
            account = new InternalUserAccount();
            account.setRepositoryId(repository);
            account.setUsername(username);

            account.setUuid(s.getSubjectId());

            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());
            account.setStatus(reg.getStatus());
            account.setEmail(reg.getEmail());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getLang());
            account.setConfirmed(reg.isConfirmed());
            account.setConfirmationDeadline(reg.getConfirmationDeadline());
            account.setConfirmationKey(reg.getConfirmationKey());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            if (logger.isTraceEnabled()) {
                logger.trace("account: {}", String.valueOf(account));
            }

            account.setAuthority(reg.getAuthority());
            account.setProvider(reg.getProvider());

            return account;
        } catch (RuntimeException e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public InternalUserAccount updateAccount(String repository, String username, InternalUserAccount reg)
        throws NoSuchUserException, RegistrationException {
        logger.debug(
            "update account with username {} in repository {}",
            String.valueOf(username),
            String.valueOf(repository)
        );

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(repository, username));
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // we support username update
            account.setUsername(reg.getUsername());

            // DISABLED uuid change
            //            // support uuid change if provided
            //            if (StringUtils.hasText(reg.getUuid())) {
            //                account.setUuid(reg.getUuid());
            //            }

            // we explode model and update every field
            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());
            account.setStatus(reg.getStatus());
            account.setEmail(reg.getEmail());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getLang());
            account.setConfirmed(reg.isConfirmed());
            account.setConfirmationDeadline(reg.getConfirmationDeadline());
            account.setConfirmationKey(reg.getConfirmationKey());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            if (logger.isTraceEnabled()) {
                logger.trace("account: {}", String.valueOf(account));
            }

            account.setAuthority(reg.getAuthority());
            account.setProvider(reg.getProvider());

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public void deleteAccount(String repository, String username) {
        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(repository, username));
        if (account != null) {
            String uuid = account.getUuid();
            if (uuid != null) {
                // remove subject if exists
                logger.debug("delete subject {} for username {}", String.valueOf(uuid), String.valueOf(username));
                subjectService.deleteSubject(uuid);
            }

            logger.debug(
                "delete account with username {} repository {}",
                String.valueOf(username),
                String.valueOf(repository)
            );

            accountRepository.delete(account);
        }
    }

    @Override
    public InternalUserAccount confirmAccount(String repository, String username, String key)
        throws NoSuchUserException, RegistrationException {
        logger.debug(
            "confirm account with username {} in repository {}",
            String.valueOf(username),
            String.valueOf(repository)
        );

        if (logger.isTraceEnabled()) {
            logger.trace("key: {}", String.valueOf(key));
        }
        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(repository, username));
        if (account == null) {
            throw new NoSuchUserException();
        }

        if (!StringUtils.hasText(key) || !key.equals(account.getConfirmationKey())) {
            throw new RegistrationException();
        }

        try {
            // override confirm
            account.setConfirmed(true);
            account.setConfirmationDeadline(null);
            account.setConfirmationKey(null);

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            if (logger.isTraceEnabled()) {
                logger.trace("account: {}", String.valueOf(account));
            }

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }
}
