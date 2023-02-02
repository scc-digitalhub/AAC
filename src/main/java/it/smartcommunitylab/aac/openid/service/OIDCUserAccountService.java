package it.smartcommunitylab.aac.openid.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountId;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

/*
 * An internal service which handles persistence for oidc user accounts, via JPA
 * 
 *  We enforce detach on fetch to keep internal datasource isolated.
 */

@Transactional
public class OIDCUserAccountService implements UserAccountService<OIDCUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OIDCUserAccountRepository accountRepository;
    private final SubjectService subjectService;

    public OIDCUserAccountService(OIDCUserAccountRepository accountRepository, SubjectService subjectService) {
        Assert.notNull(accountRepository, "account repository is required");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.accountRepository = accountRepository;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<OIDCUserAccount> findAccountByRealm(String realm) {
        logger.debug("find account for realm {}", String.valueOf(realm));

        List<OIDCUserAccount> accounts = accountRepository.findByRealm(realm);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

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

        List<OIDCUserAccount> accounts = accountRepository.findByRepositoryIdAndUsername(repository, username);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OIDCUserAccount> findAccountByEmail(String repository, String email) {
        logger.debug("find account with email {} in repository {}", String.valueOf(email),
                String.valueOf(repository));

        List<OIDCUserAccount> accounts = accountRepository.findByRepositoryIdAndEmail(repository, email);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccountByUuid(String uuid) {
        logger.debug("find account with uuid {}", String.valueOf(uuid));

        OIDCUserAccount account = accountRepository.findByUuid(uuid);
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

        List<OIDCUserAccount> accounts = accountRepository.findByUserIdAndRepositoryId(userId, repository);
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

            // create subject when needed
            String uuid = reg.getUuid();
            if (!StringUtils.hasText(uuid)) {
                uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
            }

            Subject s = subjectService.findSubject(uuid);
            if (s == null) {
                logger.debug("create new subject for sub {}", String.valueOf(subject));
                s = subjectService.addSubject(uuid, reg.getRealm(), SystemKeys.RESOURCE_ACCOUNT, subject);
            } else {
                if (!s.getRealm().equals(reg.getRealm())
                        || !SystemKeys.RESOURCE_ACCOUNT.equals(s.getType())
                        || !subject.equals(s.getSubjectId())) {
                    throw new RegistrationException("subject-mismatch");
                }
            }

            // extract attributes and build model
            account = new OIDCUserAccount();
            account.setRepositoryId(repository);
            account.setSubject(subject);
            account.setUuid(s.getSubjectId());

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
            account.setStatus(SubjectStatus.ACTIVE.getValue());

            // note: use flush because we detach the entity!
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

            account.setAuthority(reg.getAuthority());
            account.setProvider(reg.getProvider());

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public void deleteAccount(String repository, String subject) {
        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(repository, subject));
        if (account != null) {
            String uuid = account.getUuid();
            if (uuid != null) {
                // remove subject if exists
                logger.debug("delete subject {} for sub {}", String.valueOf(uuid), String.valueOf(subject));
                subjectService.deleteSubject(uuid);
            }

            logger.debug("delete account with subject {} repository {}", String.valueOf(subject),
                    String.valueOf(repository));
            accountRepository.delete(account);
        }
    }

}
