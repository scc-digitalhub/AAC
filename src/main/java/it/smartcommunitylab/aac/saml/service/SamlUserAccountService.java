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

package it.smartcommunitylab.aac.saml.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountId;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/*
 * An internal service which handles persistence for SAML2 user accounts, via JPA
 *
 *  We enforce detach on fetch to keep internal datasource isolated.
 */
@Transactional
public class SamlUserAccountService implements UserAccountService<SamlUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SamlUserAccountRepository accountRepository;
    private final SubjectService subjectService;

    public SamlUserAccountService(SamlUserAccountRepository accountRepository, SubjectService subjectService) {
        Assert.notNull(accountRepository, "account repository is required");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.accountRepository = accountRepository;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccountByRealm(String realm) {
        logger.debug("find account for realm {}", String.valueOf(realm));

        List<SamlUserAccount> accounts = accountRepository.findByRealm(realm);
        return accounts
            .stream()
            .map(a -> {
                return accountRepository.detach(a);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccountById(String repository, String subjectId) {
        logger.debug(
            "find account with subjectId {} in repository {}",
            String.valueOf(subjectId),
            String.valueOf(repository)
        );

        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(repository, subjectId));
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccountByUsername(String repository, String username) {
        logger.debug(
            "find account with username {} in repository {}",
            String.valueOf(username),
            String.valueOf(repository)
        );

        List<SamlUserAccount> accounts = accountRepository.findByRepositoryIdAndUsername(repository, username);
        return accounts
            .stream()
            .map(a -> {
                return accountRepository.detach(a);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccountByEmail(String repository, String email) {
        logger.debug("find account with email {} in repository {}", String.valueOf(email), String.valueOf(repository));

        List<SamlUserAccount> accounts = accountRepository.findByRepositoryIdAndEmail(repository, email);
        return accounts
            .stream()
            .map(a -> {
                return accountRepository.detach(a);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccountByUuid(String uuid) {
        logger.debug("find account with uuid {}", String.valueOf(uuid));

        SamlUserAccount account = accountRepository.findByUuid(uuid);
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccountByUser(String repository, String userId) {
        logger.debug("find account for user {} in repository {}", String.valueOf(userId), String.valueOf(repository));

        List<SamlUserAccount> accounts = accountRepository.findByUserIdAndRepositoryId(userId, repository);
        return accounts
            .stream()
            .map(a -> {
                return accountRepository.detach(a);
            })
            .collect(Collectors.toList());
    }

    @Override
    public SamlUserAccount addAccount(String repository, String subjectId, SamlUserAccount reg)
        throws RegistrationException {
        logger.debug(
            "add account with subjectId {} in repository {}",
            String.valueOf(subjectId),
            String.valueOf(repository)
        );

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        try {
            // check if already registered
            SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(repository, subjectId));
            if (account != null) {
                throw new DuplicatedDataException("subjectId");
            }

            // create subject when needed
            String uuid = reg.getUuid();
            if (!StringUtils.hasText(uuid)) {
                uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
            }

            Subject s = subjectService.findSubject(uuid);
            if (s == null) {
                logger.debug("create new subject for sub {}", String.valueOf(subjectId));
                s = subjectService.addSubject(uuid, reg.getRealm(), SystemKeys.RESOURCE_ACCOUNT, subjectId);
            } else {
                if (
                    !s.getRealm().equals(reg.getRealm()) ||
                    !SystemKeys.RESOURCE_ACCOUNT.equals(s.getType()) ||
                    !subjectId.equals(s.getSubjectId())
                ) {
                    throw new RegistrationException("subject-mismatch");
                }
            }

            // extract attributes and build model
            account = new SamlUserAccount(reg.getAuthority());
            account.setRepositoryId(repository);
            account.setProvider(reg.getProvider());
            account.setSubjectId(subjectId);

            account.setUuid(s.getSubjectId());

            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());

            account.setIssuer(reg.getIssuer());
            account.setUsername(reg.getUsername());
            account.setEmail(reg.getEmail());
            account.setEmailVerified(reg.getEmailVerified());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getLang());

            // set account as active
            account.setStatus(SubjectStatus.ACTIVE.getValue());

            // note: use flush because we detach the entity!
            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            account.setAuthority(reg.getAuthority());
            account.setProvider(reg.getProvider());

            if (logger.isTraceEnabled()) {
                logger.trace("account: {}", String.valueOf(account));
            }

            return account;
        } catch (RuntimeException e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public SamlUserAccount updateAccount(String repository, String subjectId, SamlUserAccount reg)
        throws NoSuchUserException, RegistrationException {
        logger.debug(
            "update account with subjectId {} in repository {}",
            String.valueOf(subjectId),
            String.valueOf(repository)
        );

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(repository, subjectId));
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // support subjectId update
            account.setSubjectId(reg.getSubjectId());

            // DISABLED support uuid change if provided
            //            if (StringUtils.hasText(reg.getUuid())) {
            //                account.setUuid(reg.getUuid());
            //            }

            // extract attributes and update model
            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());

            account.setIssuer(reg.getIssuer());
            account.setUsername(reg.getUsername());
            account.setEmail(reg.getEmail());
            account.setEmailVerified(reg.getEmailVerified());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getLang());

            // update account status
            account.setStatus(reg.getStatus());

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

    @Override
    public void deleteAccount(String repository, String subjectId) {
        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(repository, subjectId));
        if (account != null) {
            String uuid = account.getUuid();
            if (uuid != null) {
                // remove subject if exists
                logger.debug("delete subject {} for sub {}", String.valueOf(uuid), String.valueOf(subjectId));
                subjectService.deleteSubject(uuid);
            }

            logger.debug(
                "delete account with subjectId {} repository {}",
                String.valueOf(subjectId),
                String.valueOf(repository)
            );
            accountRepository.delete(account);
        }
    }
}
