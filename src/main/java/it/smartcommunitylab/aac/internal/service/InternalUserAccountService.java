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
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountId;
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

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUsername(String provider, String username) {
        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(provider, username));
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findAccountByEmail(String provider, String email) {
        List<InternalUserAccount> accounts = accountRepository.findByProviderAndEmail(provider, email);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByConfirmationKey(String provider, String key) {
        InternalUserAccount account = accountRepository.findByProviderAndConfirmationKey(provider, key);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByResetKey(String provider, String key) {
        InternalUserAccount account = accountRepository.findByProviderAndResetKey(provider, key);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findByUser(String userId) {
        List<InternalUserAccount> accounts = accountRepository.findByUserId(userId);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InternalUserAccount> findByUser(String userId, String provider) {
        List<InternalUserAccount> accounts = accountRepository.findByUserIdAndProvider(userId, provider);
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
            account.setProvider(reg.getProvider());
            account.setUsername(reg.getUsername());
            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());
            account.setPassword(reg.getPassword());
            account.setStatus(reg.getStatus());
            account.setEmail(reg.getEmail());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getLang());
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

    public InternalUserAccount updateAccount(
            String provider, String username,
            InternalUserAccount reg) throws NoSuchUserException, RegistrationException {
        if (reg == null) {
            throw new NoSuchUserException();
        }

        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(provider, username));
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // we support username update
            account.setUsername(reg.getUsername());

            // we explode model and update every field
            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());
            account.setPassword(reg.getPassword());
            account.setStatus(reg.getStatus());
            account.setEmail(reg.getEmail());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getLang());
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

    public void deleteAccount(String provider, String username) {
        InternalUserAccount account = accountRepository.findOne(new InternalUserAccountId(provider, username));
        if (account != null) {
            accountRepository.delete(account);
        }

    }

}
