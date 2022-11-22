package it.smartcommunitylab.aac.password.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;

/*
 * An internal service which handles password persistence for internal user accounts, via JPA.
 * 
 * We enforce detach on fetch to keep internal datasource isolated. 
 */
@Transactional
public class InternalPasswordUserCredentialsService implements UserCredentialsService<InternalUserPassword> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalUserPasswordRepository passwordRepository;

    public InternalPasswordUserCredentialsService(InternalUserPasswordRepository passwordRepository) {
        Assert.notNull(passwordRepository, "password repository is mandatory");
        this.passwordRepository = passwordRepository;
    }

    @Override
    public List<InternalUserPassword> findCredentialsByRealm(@NotNull String realm) {
        logger.debug("find credentials for realm {}", String.valueOf(realm));

        List<InternalUserPassword> passwords = passwordRepository.findByRealm(realm);
        return passwords.stream().map(a -> {
            return passwordRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Override
    public InternalUserPassword findCredentialsById(@NotNull String repository, @NotNull String id) {
        logger.debug("find credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        InternalUserPassword password = passwordRepository.findOne(id);
        if (password == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return passwordRepository.detach(password);
    }

    @Override
    public InternalUserPassword findCredentialsByUuid(@NotNull String uuid) {
        logger.debug("find credentials with uuid {}", String.valueOf(uuid));

        // uuid is id
        InternalUserPassword password = passwordRepository.findOne(uuid);
        if (password == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return passwordRepository.detach(password);
    }

    @Override
    public List<InternalUserPassword> findCredentialsByAccount(@NotNull String repository, @NotNull String accountId) {
        logger.debug("find credentials for account {} in repository {}", String.valueOf(accountId),
                String.valueOf(repository));

        List<InternalUserPassword> passwords = passwordRepository
                .findByRepositoryIdAndUsernameOrderByCreateDateDesc(repository, accountId);
        return passwords.stream().map(a -> {
            return passwordRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Override
    public List<InternalUserPassword> findCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        logger.debug("find credentials for user {} in repository {}", String.valueOf(userId),
                String.valueOf(repository));

        List<InternalUserPassword> passwords = passwordRepository.findByRepositoryIdAndUserId(repository, userId);
        return passwords.stream().map(a -> {
            return passwordRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Override
    public InternalUserPassword addCredentials(@NotNull String repository, @NotNull String id,
            @NotNull InternalUserPassword reg) throws RegistrationException {
        logger.debug("add credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        try {
            // check if already registered
            InternalUserPassword password = passwordRepository.findOne(id);
            if (password != null) {
                throw new DuplicatedDataException("id");
            }

            // create password already hashed
            password = new InternalUserPassword();
            password.setId(id);
            password.setRepositoryId(repository);

            password.setUsername(reg.getUsername());
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

            // password are encrypted, return as is
            password = passwordRepository.detach(password);

            password.setAuthority(reg.getAuthority());
            password.setProvider(reg.getProvider());

            if (logger.isTraceEnabled()) {
                logger.trace("password: {}", String.valueOf(password));
            }

            return password;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public InternalUserPassword updateCredentials(@NotNull String repository, @NotNull String id,
            @NotNull InternalUserPassword reg) throws NoSuchCredentialException, RegistrationException {
        logger.debug("update credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }
        InternalUserPassword password = passwordRepository.findOne(id);
        if (password == null) {
            throw new NoSuchCredentialException();
        }

        try {
            // set update fields
            password.setUsername(reg.getUsername());
            password.setUserId(reg.getUserId());
            password.setRealm(reg.getRealm());

            password.setPassword(reg.getPassword());
            password.setChangeOnFirstAccess(reg.getChangeOnFirstAccess());
            password.setExpirationDate(reg.getExpirationDate());

            password.setResetDeadline(null);
            password.setResetKey(null);

            password.setStatus(reg.getStatus());

            // note: use flush because we detach the entity!
            password = passwordRepository.saveAndFlush(password);

            // password are encrypted, return as is
            password = passwordRepository.detach(password);

            if (logger.isTraceEnabled()) {
                logger.trace("password: {}", String.valueOf(password));
            }

            password.setAuthority(reg.getAuthority());
            password.setProvider(reg.getProvider());

            return password;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public void deleteCredentials(@NotNull String repository, @NotNull String id) {
        InternalUserPassword password = passwordRepository.findOne(id);
        if (password != null) {
            logger.debug("delete password with id {} repository {}", String.valueOf(id), String.valueOf(repository));
            passwordRepository.delete(password);
        }

    }
}
