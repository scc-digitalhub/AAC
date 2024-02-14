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

package it.smartcommunitylab.aac.password.service;

import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.credentials.persistence.UserCredentialsService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.password.model.InternalUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordEntity;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordEntityRepository;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/*
 * An internal service which handles password persistence for internal user accounts, via JPA.
 *
 * We enforce detach on fetch to keep internal datasource isolated.
 */
@Transactional
public class InternalPasswordJpaUserCredentialsService implements UserCredentialsService<InternalUserPassword> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalUserPasswordEntityRepository passwordRepository;

    public InternalPasswordJpaUserCredentialsService(InternalUserPasswordEntityRepository passwordRepository) {
        Assert.notNull(passwordRepository, "password repository is mandatory");
        this.passwordRepository = passwordRepository;
    }

    @Override
    public Collection<InternalUserPassword> findCredentials(@NotNull String repositoryId) {
        logger.debug("find credentials for repository {}", String.valueOf(repositoryId));

        List<InternalUserPasswordEntity> credentials = passwordRepository.findByRepositoryId(repositoryId);
        return credentials.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalUserPassword> findCredentialsByRealm(@NotNull String realm) {
        logger.debug("find credentials for realm {}", String.valueOf(realm));

        List<InternalUserPasswordEntity> passwords = passwordRepository.findByRealm(realm);
        return passwords.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserPassword findCredentialsById(@NotNull String repository, @NotNull String id) {
        logger.debug("find credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        InternalUserPasswordEntity password = passwordRepository.findOne(id);
        if (password == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return to(password);
    }

    @Transactional(readOnly = true)
    public InternalUserPassword findCredentialsByResetKey(@NotNull String repository, @NotNull String key) {
        logger.debug(
            "find credentials with reset key {} in repository {}",
            String.valueOf(key),
            String.valueOf(repository)
        );

        InternalUserPasswordEntity password = passwordRepository.findByRepositoryIdAndResetKey(repository, key);
        if (password == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return to(password);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserPassword findCredentialsByUuid(@NotNull String uuid) {
        logger.debug("find credentials with uuid {}", String.valueOf(uuid));

        // uuid is id
        InternalUserPasswordEntity password = passwordRepository.findOne(uuid);
        if (password == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return to(password);
    }

    // @Override
    // @Transactional(readOnly = true)
    // public List<InternalUserPassword> findCredentialsByAccount(@NotNull String repository, @NotNull String accountId) {
    //     logger.debug(
    //         "find credentials for account {} in repository {}",
    //         String.valueOf(accountId),
    //         String.valueOf(repository)
    //     );

    //     List<InternalUserPasswordEntity> passwords =
    //         passwordRepository.findByRepositoryIdAndUsernameOrderByCreateDateDesc(repository, accountId);
    //     return passwords
    //         .stream()
    //         .map(a -> {
    //             return passwordRepository.detach(a);
    //         })
    //         .collect(Collectors.toList());
    // }

    @Override
    @Transactional(readOnly = true)
    public List<InternalUserPassword> findCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        logger.debug(
            "find credentials for user {} in repository {}",
            String.valueOf(userId),
            String.valueOf(repository)
        );

        List<InternalUserPasswordEntity> passwords = passwordRepository.findByRepositoryIdAndUserId(repository, userId);
        return passwords.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Override
    public InternalUserPassword addCredentials(
        @NotNull String repository,
        @NotNull String id,
        @NotNull InternalUserPassword reg
    ) throws RegistrationException {
        logger.debug("add credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        try {
            // check if already registered
            InternalUserPasswordEntity password = passwordRepository.findOne(id);
            if (password != null) {
                throw new DuplicatedDataException("id");
            }

            // create password already hashed
            password = new InternalUserPasswordEntity();
            password.setId(id);
            password.setRepositoryId(repository);

            // password.setUsername(reg.getUsername());
            password.setUserId(reg.getUserId());
            password.setRealm(reg.getRealm());

            password.setPassword(reg.getPassword());
            password.setChangeOnFirstAccess(reg.getChangeOnFirstAccess());
            password.setExpirationDate(reg.getExpirationDate());

            password.setResetDeadline(null);
            password.setResetKey(null);

            // set status as active
            password.setStatus(CredentialsStatus.ACTIVE.getValue());

            // note: use flush because we detach the entity!
            password = passwordRepository.saveAndFlush(password);

            if (logger.isTraceEnabled()) {
                logger.trace("password: {}", String.valueOf(password));
            }

            // password are encrypted, return as is
            InternalUserPassword p = to(password);
            p.setAuthority(reg.getAuthority());
            p.setProvider(reg.getProvider());

            return p;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public InternalUserPassword updateCredentials(
        @NotNull String repository,
        @NotNull String id,
        @NotNull InternalUserPassword reg
    ) throws NoSuchCredentialException, RegistrationException {
        logger.debug("update credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }
        InternalUserPasswordEntity password = passwordRepository.findOne(id);
        if (password == null) {
            throw new NoSuchCredentialException();
        }

        try {
            // set update fields
            // password.setUsername(reg.getUsername());
            password.setUserId(reg.getUserId());
            password.setRealm(reg.getRealm());

            password.setPassword(reg.getPassword());
            password.setChangeOnFirstAccess(reg.getChangeOnFirstAccess());
            password.setExpirationDate(reg.getExpirationDate());

            password.setResetDeadline(reg.getResetDeadline());
            password.setResetKey(reg.getResetKey());

            password.setStatus(reg.getStatus());

            // note: use flush because we detach the entity!
            password = passwordRepository.saveAndFlush(password);

            if (logger.isTraceEnabled()) {
                logger.trace("password: {}", String.valueOf(password));
            }
            // password are encrypted, return as is
            InternalUserPassword p = to(password);
            p.setAuthority(reg.getAuthority());
            p.setProvider(reg.getProvider());

            return p;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public void deleteCredentials(@NotNull String repository, @NotNull String id) {
        InternalUserPasswordEntity password = passwordRepository.findOne(id);
        if (password != null) {
            logger.debug("delete password with id {} repository {}", String.valueOf(id), String.valueOf(repository));
            passwordRepository.delete(password);
        }
    }

    @Override
    public void deleteAllCredentials(@NotNull String repository, @NotNull Collection<String> ids) {
        logger.debug("delete passwords with id in {} repository {}", String.valueOf(ids), String.valueOf(repository));
        passwordRepository.deleteAllByIdInBatch(ids);
    }

    @Override
    public void deleteAllCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        logger.debug(
            "delete credentials for user {} in repository {}",
            String.valueOf(userId),
            String.valueOf(repository)
        );

        List<InternalUserPasswordEntity> passwords = passwordRepository.findByRepositoryIdAndUserId(repository, userId);
        passwordRepository.deleteAllInBatch(passwords);
    }

    /*
     * Helpers
     * TODO converters?
     */

    private InternalUserPassword to(InternalUserPasswordEntity e) {
        InternalUserPassword c = new InternalUserPassword(e.getRealm(), e.getId());
        c.setRepositoryId(e.getRepositoryId());
        // c.setUsername(e.getUsername());
        c.setUserId(e.getUserId());

        c.setPassword(e.getPassword());

        c.setCreateDate(e.getCreateDate());
        c.setExpirationDate(e.getExpirationDate());
        c.setResetDeadline(e.getResetDeadline());
        c.setResetKey(e.getResetKey());
        c.setChangeOnFirstAccess(e.getChangeOnFirstAccess());

        c.setStatus(e.getStatus());

        return c;
    }
}
