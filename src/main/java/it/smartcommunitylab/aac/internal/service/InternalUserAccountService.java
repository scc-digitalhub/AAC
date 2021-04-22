package it.smartcommunitylab.aac.internal.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

/*
 * An internal service which handles persistence for internal user accounts, via JPA
 * 
 *  We enforce detach on fetch to keep internal datasource isolated.
 */
@Service
@Transactional
public class InternalUserAccountService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private InternalUserAccountRepository accountRepository;

    public InternalUserAccount getAccount(long id) throws NoSuchUserException {
        InternalUserAccount account = accountRepository.findOne(id);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return accountRepository.detach(account);
    }

    public InternalUserAccount findAccount(long id) {
        InternalUserAccount account = accountRepository.findOne(id);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    public InternalUserAccount findAccountByUsername(String realm, String username) {
        InternalUserAccount account = accountRepository.findByRealmAndUsername(realm, username);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    public InternalUserAccount findAccountByEmail(String realm, String email) {
        InternalUserAccount account = accountRepository.findByRealmAndEmail(realm, email);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    public InternalUserAccount findAccountByConfirmationKey(String key) {
        InternalUserAccount account = accountRepository.findByConfirmationKey(key);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    public InternalUserAccount findAccountByResetKey(String key) {
        InternalUserAccount account = accountRepository.findByResetKey(key);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    public List<InternalUserAccount> findBySubject(String subject) {
        List<InternalUserAccount> accounts = accountRepository.findBySubject(subject);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    public List<InternalUserAccount> findBySubject(String subject, String realm) {
        List<InternalUserAccount> accounts = accountRepository.findBySubjectAndRealm(subject, realm);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    /*
     * CRUD
     */
    public InternalUserAccount addAccount(
            InternalUserAccount reg) throws RegistrationException {

        try {
            // we explode model
            InternalUserAccount account = new InternalUserAccount();
            account.setSubject(reg.getSubject());
            account.setRealm(reg.getRealm());
            account.setUsername(reg.getUsername());
            account.setPassword(reg.getPassword());
            account.setEmail(reg.getEmail());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getSurname());
            account.setConfirmed(reg.isConfirmed());
            account.setConfirmationDeadline(reg.getConfirmationDeadline());
            account.setConfirmationKey(reg.getConfirmationKey());
            account.setResetDeadline(reg.getResetDeadline());
            account.setResetKey(reg.getResetKey());
            account.setChangeOnFirstAccess(reg.getChangeOnFirstAccess());

            account = accountRepository.save(account);
            account = accountRepository.detach(account);

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public InternalUserAccount updateAccount(
            long id,
            InternalUserAccount reg) throws NoSuchUserException, RegistrationException {
        if (reg == null) {
            throw new NoSuchUserException();
        }

        InternalUserAccount account = accountRepository.findOne(id);
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // we explode model and update every field
            account.setSubject(reg.getSubject());
            account.setRealm(reg.getRealm());
            account.setUsername(reg.getUsername());
            account.setPassword(reg.getPassword());
            account.setEmail(reg.getEmail());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getSurname());
            account.setConfirmed(reg.isConfirmed());
            account.setConfirmationDeadline(reg.getConfirmationDeadline());
            account.setConfirmationKey(reg.getConfirmationKey());
            account.setResetDeadline(reg.getResetDeadline());
            account.setResetKey(reg.getResetKey());
            account.setChangeOnFirstAccess(reg.getChangeOnFirstAccess());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }

    }

    public void deleteAccount(long id) {
        InternalUserAccount account = accountRepository.findOne(id);
        if (account != null) {
            accountRepository.delete(account);
        }

    }

}
