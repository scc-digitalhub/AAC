package it.smartcommunitylab.aac.openid.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountId;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

/*
 * An internal service which handles persistence for oidc user accounts, via JPA
 * 
 *  We enforce detach on fetch to keep internal datasource isolated.
 */
@Service
@Transactional
public class OIDCUserAccountService implements UserAccountService<OIDCUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OIDCUserAccountRepository accountRepository;

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccountById(String repository, String subject) {
        logger.debug("find account with subject {} in repository {}", String.valueOf(subject),
                String.valueOf(repository));

        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(repository, subject));
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<OIDCUserAccount> findAccountByUsername(String repository, String username) {
        logger.debug("find account with username {} in repository {}", String.valueOf(username),
                String.valueOf(repository));

        List<OIDCUserAccount> accounts = accountRepository.findByProviderAndUsername(repository, username);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OIDCUserAccount> findAccountByEmail(String repository, String email) {
        logger.debug("find account with email {} in repository {}", String.valueOf(email),
                String.valueOf(repository));

        List<OIDCUserAccount> accounts = accountRepository.findByProviderAndEmail(repository, email);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccountByUuid(String repository, String uuid) {
        logger.debug("find account with uuid {} in repository {}", String.valueOf(uuid),
                String.valueOf(repository));

        OIDCUserAccount account = accountRepository.findByProviderAndUuid(repository, uuid);
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<OIDCUserAccount> findAccountByUser(String repository, String userId) {
        logger.debug("find account for user {} in repository {}", String.valueOf(userId),
                String.valueOf(repository));

        List<OIDCUserAccount> accounts = accountRepository.findByUserIdAndProvider(userId, repository);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Override
    public OIDCUserAccount addAccount(String repository, String subject, OIDCUserAccount reg)
            throws RegistrationException {
        logger.debug("add account with subject {} in repository {}", String.valueOf(subject),
                String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        try {
            // check if already registered
            OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(repository, subject));
            if (account != null) {
                throw new DuplicatedDataException("subject");
            }

            // extract attributes and build model
            account = new OIDCUserAccount(reg.getAuthority());
            account.setProvider(repository);
            account.setSubject(subject);

            account.setUuid(reg.getUuid());
            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());

            account.setIssuer(reg.getIssuer());
            account.setUsername(reg.getUsername());
            account.setEmail(reg.getEmail());
            account.setEmailVerified(reg.getEmailVerified());
            account.setName(reg.getName());
            account.setGivenName(reg.getGivenName());
            account.setFamilyName(reg.getFamilyName());
            account.setLang(reg.getLang());
            account.setPicture(reg.getPicture());

            // set account as active
            account.setStatus(UserStatus.ACTIVE.getValue());

            account = accountRepository.save(account);
            account = accountRepository.detach(account);

            if (logger.isTraceEnabled()) {
                logger.trace("account: {}", String.valueOf(account));
            }

            return account;

        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public OIDCUserAccount updateAccount(String repository, String subject, OIDCUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        logger.debug("update account with subject {} in repository {}", String.valueOf(subject),
                String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(repository, subject));
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // support subject update
            account.setSubject(reg.getSubject());

            // extract attributes and update model
            account.setUuid(reg.getUuid());
            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());

            account.setIssuer(reg.getIssuer());
            account.setUsername(reg.getUsername());
            account.setEmail(reg.getEmail());
            account.setEmailVerified(reg.getEmailVerified());
            account.setName(reg.getName());
            account.setGivenName(reg.getGivenName());
            account.setFamilyName(reg.getFamilyName());
            account.setLang(reg.getLang());
            account.setPicture(reg.getPicture());

            // update account status
            account.setStatus(reg.getStatus());

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

    @Override
    public void deleteAccount(String repository, String subject) {
        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(repository, subject));
        if (account != null) {
            logger.debug("delete account with subject {} repository {}", String.valueOf(subject),
                    String.valueOf(repository));
            accountRepository.delete(account);
        }
    }

}
