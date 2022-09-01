package it.smartcommunitylab.aac.internal.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountId;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

/*
 * An internal service which handles persistence for internal user accounts, via JPA
 * 
 *  We enforce detach on fetch to keep internal datasource isolated.
 */

@Transactional
public class InternalUserAccountService
        implements UserAccountService<InternalUserAccount>, InternalUserConfirmKeyService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalUserAccountRepository accountRepository;

    public InternalUserAccountService(InternalUserAccountRepository accountRepository) {
        Assert.notNull(accountRepository, "account repository is required");
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountById(String repository, String username) {
        logger.debug("find account with username {} in repository {}", String.valueOf(username),
                String.valueOf(repository));

        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(repository, username));
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findAccountByUsername(String repository, String username) {
        logger.debug("find account with username {} in repository {}", String.valueOf(username),
                String.valueOf(repository));

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
        logger.debug("find account with email {} in repository {}", String.valueOf(email),
                String.valueOf(repository));

        List<InternalUserAccount> accounts = accountRepository.findByProviderAndEmail(repository, email);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByConfirmationKey(String repository, String key) {
        logger.debug("find account with confirmation key {} in repository {}", String.valueOf(key),
                String.valueOf(repository));

        InternalUserAccount account = accountRepository.findByProviderAndConfirmationKey(repository, key);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUuid(String repository, String uuid) {
        logger.debug("find account with uuid {} in repository {}", String.valueOf(uuid),
                String.valueOf(repository));

        InternalUserAccount account = accountRepository.findByProviderAndUuid(repository, uuid);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findAccountByUser(String repository, String userId) {
        logger.debug("find account for user {} in repository {}", String.valueOf(userId),
                String.valueOf(repository));

        List<InternalUserAccount> accounts = accountRepository.findByUserIdAndProvider(userId, repository);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    /*
     * CRUD
     */
    public InternalUserAccount addAccount(
            String repository, String username,
            InternalUserAccount reg) throws RegistrationException {
        logger.debug("add account with username {} in repository {}", String.valueOf(username),
                String.valueOf(repository));

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

            // we explode model
            account = new InternalUserAccount(reg.getAuthority());
            account.setProvider(repository);
            account.setUsername(username);

            account.setUuid(reg.getUuid());
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

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public InternalUserAccount updateAccount(
            String repository, String username,
            InternalUserAccount reg) throws NoSuchUserException, RegistrationException {
        logger.debug("update account with username {} in repository {}", String.valueOf(username),
                String.valueOf(repository));

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

            // support uuid change if provided
            if (StringUtils.hasText(reg.getUuid())) {
                account.setUuid(reg.getUuid());
            }

            // we explode model and update every field
            account.setAuthority(reg.getAuthority());
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

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public void deleteAccount(String repository, String username) {
        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(repository, username));
        if (account != null) {
            logger.debug("delete account with username {} repository {}", String.valueOf(username),
                    String.valueOf(repository));

            accountRepository.delete(account);
        }

    }

}
