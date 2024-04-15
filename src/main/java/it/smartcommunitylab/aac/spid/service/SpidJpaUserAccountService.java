/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountEntity;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountId;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountEntity;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountEntityRepository;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountId;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class SpidJpaUserAccountService implements UserAccountService<SpidUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SpidUserAccountEntityRepository accountRepository;
    private final SubjectService subjectService;

    public SpidJpaUserAccountService(SpidUserAccountEntityRepository accountRepository, SubjectService subjectService) {
        Assert.notNull(accountRepository, "spid account repository is required");
        Assert.notNull(subjectService, "subject service is mandatory");
        this.accountRepository = accountRepository;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccounts(String repositoryId) {
        logger.debug("find account for repositoryId {}", String.valueOf(repositoryId));

        List<SpidUserAccountEntity> accounts = accountRepository.findByRepositoryId(repositoryId);
        return accounts.stream().map(this::to).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccountsByRealm(String realm) {
        logger.debug("find account for realm {}", String.valueOf(realm));

        List<SpidUserAccountEntity> accounts = accountRepository.findByRealm(realm);
        return accounts.stream().map(this::to).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpidUserAccount findAccountById(String repository, String subjectId) {
        logger.debug(
            "find account with subjectId {} in repository {}",
            String.valueOf(subjectId),
            String.valueOf(repository)
        );
        SpidUserAccountEntity entity = accountRepository.findOne(new SpidUserAccountId(repository, subjectId));
        if (entity == null) {
            return null;
        }
        return to(entity);
    }

    @Transactional(readOnly = true)
    public SpidUserAccount findAccountByUuid(String uuid) {
        logger.debug("find account with uuid {}", String.valueOf(uuid));
        SpidUserAccountEntity account = accountRepository.findByUuid(uuid);
        if (account == null) {
            return null;
        }
        return to(account);
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccountsByUsername(String repository, String username) {
        logger.debug(
            "find account with username {} in repository {}",
            String.valueOf(username),
            String.valueOf(repository)
        );
        List<SpidUserAccountEntity> accounts = accountRepository.findByRepositoryIdAndUsername(repository, username);
        return accounts.stream().map(this::to).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccountsByEmail(String repository, String email) {
        logger.debug("find account with email {} in repository {}", String.valueOf(email), String.valueOf(repository));

        List<SpidUserAccountEntity> accounts = accountRepository.findByRepositoryIdAndEmail(repository, email);
        return accounts.stream().map(this::to).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccountsByUser(String repository, String userId) {
        logger.debug("find account for user {} in repository {}", String.valueOf(userId), String.valueOf(repository));

        List<SpidUserAccountEntity> accounts = accountRepository.findByUserIdAndRepositoryId(userId, repository);
        return accounts.stream().map(this::to).collect(Collectors.toList());
    }

    @Override
    public SpidUserAccount addAccount(String repository, String subjectId, SpidUserAccount reg)
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
        SpidUserAccount a;
        try {
            // check if already registered
            SpidUserAccountEntity entity = accountRepository.findOne(new SpidUserAccountId(repository, subjectId));
            if (entity != null) {
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

            // build entity model from the new account
            entity = new SpidUserAccountEntity();
            entity.setRepositoryId(repository);
            entity.setSubjectId(subjectId);
            entity.setUuid(s.getSubjectId());

            entity.setUserId(reg.getUserId());
            entity.setRealm(reg.getRealm());
            entity.setUsername(reg.getUsername());
            entity.setIdp(reg.getIdp());
            entity.setSpidCode(reg.getSpidCode());
            entity.setEmail(reg.getEmail());
            entity.setName(reg.getName());
            entity.setSurname(reg.getSurname());
            entity.setPhone(reg.getPhone());
            entity.setFiscalNumber(reg.getFiscalNumber());
            entity.setIvaCode(reg.getIvaCode());

            // set account as active
            entity.setStatus(SubjectStatus.ACTIVE.getValue());

            //copy audit info for import/export
            entity.setCreateDate(reg.getCreateDate());
            entity.setModifiedDate(reg.getModifiedDate());

            // note: use flush because we detach the entity!
            entity = accountRepository.saveAndFlush(entity);

            a = to(entity);
            a.setAuthority(reg.getAuthority());
            a.setProvider(reg.getProvider());
        } catch (RuntimeException e) {
            throw new RegistrationException(e.getMessage());
        }
        return a;
    }

    @Override
    public SpidUserAccount updateAccount(String repository, String subjectId, SpidUserAccount reg)
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

        SpidUserAccountEntity entity = accountRepository.findOne(new SpidUserAccountId(repository, subjectId));
        if (entity == null) {
            throw new NoSuchUserException();
        }
        SpidUserAccount account;
        try {
            entity.setUserId(reg.getUserId());
            entity.setRealm(reg.getRealm());
            entity.setUsername(reg.getUsername());
            entity.setIdp(reg.getIdp());
            entity.setSpidCode(reg.getSpidCode());
            entity.setEmail(reg.getEmail());
            entity.setName(reg.getName());
            entity.setSurname(reg.getSurname());
            entity.setPhone(reg.getPhone());
            entity.setFiscalNumber(reg.getFiscalNumber());
            entity.setIvaCode(reg.getIvaCode());

            // set account as active
            entity.setStatus(reg.getStatus());
            entity = accountRepository.saveAndFlush(entity);

            // TODO: aggiornare la modified date?

            if (logger.isTraceEnabled()) {
                logger.trace("account: {}", String.valueOf(entity));
            }

            account = to(entity);
            account.setAuthority(reg.getAuthority());
            account.setProvider(reg.getProvider());
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
        return account;
    }

    @Override
    public void deleteAccount(String repository, String subjectId) {
        SpidUserAccountEntity account = accountRepository.findOne(new SpidUserAccountId(repository, subjectId));
        if (account == null) {
            return;
        }
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

    @Override
    public void deleteAllAccountsByUser(String repository, String userId) {
        logger.debug(
            "delete accounts for user {} in repository {}",
            String.valueOf(userId),
            String.valueOf(repository)
        );

        List<SpidUserAccountEntity> accounts = accountRepository.findByUserIdAndRepositoryId(userId, repository);
        accountRepository.deleteAllInBatch(accounts);
    }

    private SpidUserAccount to(SpidUserAccountEntity entity) {
        SpidUserAccount account = new SpidUserAccount(null, entity.getRealm(), entity.getUuid());

        account.setRepositoryId(entity.getRepositoryId());
        account.setSubjectId(entity.getSubjectId());
        account.setUuid(entity.getUuid());

        account.setUserId(entity.getUserId());
        account.setStatus(entity.getStatus());

        account.setUsername(entity.getUsername());
        account.setEmail(entity.getEmail());
        account.setName(entity.getName());
        account.setSurname(entity.getSurname());
        account.setSpidCode(entity.getSpidCode());
        account.setPhone(entity.getPhone());
        account.setIdp(entity.getIdp());
        account.setFiscalNumber(entity.getFiscalNumber());
        account.setIvaCode(entity.getIvaCode());

        account.setAttributes(entity.getAttributes());
        account.setCreateDate(entity.getCreateDate());
        account.setModifiedDate(entity.getModifiedDate());

        return account;
    }
}
