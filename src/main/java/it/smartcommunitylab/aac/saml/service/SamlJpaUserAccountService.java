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
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountEntity;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountEntityRepository;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountId;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
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
public class SamlJpaUserAccountService implements UserAccountService<SamlUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SamlUserAccountEntityRepository accountRepository;
    private final SubjectService subjectService;

    public SamlJpaUserAccountService(SamlUserAccountEntityRepository accountRepository, SubjectService subjectService) {
        Assert.notNull(accountRepository, "account repository is required");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.accountRepository = accountRepository;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccounts(String repositoryId) {
        logger.debug("find account for repositoryId {}", String.valueOf(repositoryId));

        List<SamlUserAccountEntity> accounts = accountRepository.findByRepositoryId(repositoryId);
        return accounts.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccountsByRealm(String realm) {
        logger.debug("find account for realm {}", String.valueOf(realm));

        List<SamlUserAccountEntity> accounts = accountRepository.findByRealm(realm);
        return accounts.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccountById(String repository, String subjectId) {
        logger.debug(
            "find account with subjectId {} in repository {}",
            String.valueOf(subjectId),
            String.valueOf(repository)
        );

        SamlUserAccountEntity account = accountRepository.findOne(new SamlUserAccountId(repository, subjectId));
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return to(account);
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccountsByUsername(String repository, String username) {
        logger.debug(
            "find account with username {} in repository {}",
            String.valueOf(username),
            String.valueOf(repository)
        );

        List<SamlUserAccountEntity> accounts = accountRepository.findByRepositoryIdAndUsername(repository, username);
        return accounts.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccountsByEmail(String repository, String email) {
        logger.debug("find account with email {} in repository {}", String.valueOf(email), String.valueOf(repository));

        List<SamlUserAccountEntity> accounts = accountRepository.findByRepositoryIdAndEmail(repository, email);
        return accounts.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccountByUuid(String uuid) {
        logger.debug("find account with uuid {}", String.valueOf(uuid));

        SamlUserAccountEntity account = accountRepository.findByUuid(uuid);
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return to(account);
    }

    @Transactional(readOnly = true)
    public List<SamlUserAccount> findAccountsByUser(String repository, String userId) {
        logger.debug("find account for user {} in repository {}", String.valueOf(userId), String.valueOf(repository));

        List<SamlUserAccountEntity> accounts = accountRepository.findByUserIdAndRepositoryId(userId, repository);
        return accounts.stream().map(a -> to(a)).collect(Collectors.toList());
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
            SamlUserAccountEntity account = accountRepository.findOne(new SamlUserAccountId(repository, subjectId));
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
            account = new SamlUserAccountEntity();
            account.setRepositoryId(repository);
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

            account.setAttributes(reg.getAttributes());

            // set account as active
            account.setStatus(SubjectStatus.ACTIVE.getValue());

            //copy audit info for import/export
            account.setCreateDate(reg.getCreateDate());
            account.setModifiedDate(reg.getModifiedDate());

            // note: use flush because we detach the entity!
            account = accountRepository.saveAndFlush(account);

            if (logger.isTraceEnabled()) {
                logger.trace("account: {}", String.valueOf(account));
            }

            //convert and set transient fields
            SamlUserAccount a = to(account);
            a.setAuthority(reg.getAuthority());
            a.setProvider(reg.getProvider());

            return a;
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

        SamlUserAccountEntity account = accountRepository.findOne(new SamlUserAccountId(repository, subjectId));
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // DISABLED support subjectId update
            // account.setSubjectId(reg.getSubjectId());

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

            account.setAttributes(reg.getAttributes());

            // update account status
            account.setStatus(reg.getStatus());

            account = accountRepository.saveAndFlush(account);

            if (logger.isTraceEnabled()) {
                logger.trace("account: {}", String.valueOf(account));
            }

            //convert and set transient fields
            SamlUserAccount a = to(account);
            a.setAuthority(reg.getAuthority());
            a.setProvider(reg.getProvider());

            return a;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public void deleteAccount(String repository, String subjectId) {
        SamlUserAccountEntity account = accountRepository.findOne(new SamlUserAccountId(repository, subjectId));
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

    @Override
    public void deleteAllAccountsByUser(@NotNull String repository, @NotNull String userId) {
        logger.debug(
            "delete accounts for user {} in repository {}",
            String.valueOf(userId),
            String.valueOf(repository)
        );

        List<SamlUserAccountEntity> accounts = accountRepository.findByUserIdAndRepositoryId(userId, repository);
        accountRepository.deleteAllInBatch(accounts);
    }

    @Override
    public void deleteAllAccountsByRealm(@NotNull String realm) {
        logger.debug("delete accounts for realm {} in all repositories", String.valueOf(realm));

        List<SamlUserAccountEntity> accounts = accountRepository.findByRealm(realm);
        accountRepository.deleteAllInBatch(accounts);
    }

    /*
     * Helpers
     * TODO converters?
     */

    private SamlUserAccount to(SamlUserAccountEntity entity) {
        //note: transient fields are set to null, they will be populated by providers
        SamlUserAccount account = new SamlUserAccount(null, null, entity.getRealm(), entity.getUuid());

        account.setRepositoryId(entity.getRepositoryId());
        account.setSubjectId(entity.getSubjectId());
        account.setUuid(entity.getUuid());

        account.setUserId(entity.getUserId());
        account.setStatus(entity.getStatus());

        account.setIssuer(entity.getIssuer());
        account.setUsername(entity.getUsername());
        account.setEmail(entity.getEmail());
        account.setEmailVerified(entity.getEmailVerified());
        account.setName(entity.getName());
        account.setSurname(entity.getSurname());
        account.setLang(entity.getLang());

        account.setAttributes(entity.getAttributes());

        account.setCreateDate(entity.getCreateDate());
        account.setModifiedDate(entity.getModifiedDate());

        return account;
    }
}
