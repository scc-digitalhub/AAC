package it.smartcommunitylab.aac.internal.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

@Service
@Transactional
public class InternalUserService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private InternalUserAccountRepository accountRepository;

    /*
     * Get
     */
    public String getSubject(String realm, String username) throws NoSuchUserException {
        InternalUserAccount account = getAccount(realm, username);
        return account.getSubject();

//        UserEntity user = userRepository.getUser(account.getSubject());
//
//        if (user == null) {
//            throw new NoSuchUserException();
//        }
//
//        return user.getUuid();
    }

    public InternalUserAccount findAccount(String userId) {
        // userId is numeric
        return accountRepository.findByUserId(Long.parseLong(userId));
    }

    public InternalUserAccount findAccount(String realm, String username) {
        return accountRepository.findByRealmAndUsername(realm, username);

    }

    public InternalUserAccount getAccount(String realm, String username) throws NoSuchUserException {
        InternalUserAccount account = accountRepository.findByRealmAndUsername(realm, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    public InternalUserAccount getAccountByConfirmationKey(String key) throws NoSuchUserException {
        InternalUserAccount account = accountRepository.findByConfirmationKey(key);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    public InternalUserAccount getAccountByResetKey(String key) throws NoSuchUserException {
        InternalUserAccount account = accountRepository.findByResetKey(key);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    /*
     * CRUD
     */
    public InternalUserAccount addAccount(
            String subject,
            String realm,
            String username,
            String email,
            String name,
            String surname,
            String lang) {

        InternalUserAccount account = new InternalUserAccount();
        account.setSubject(subject);
        account.setRealm(realm);
        account.setUsername(username);
        // by default disable login
        account.setPassword(null);
        account.setEmail(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);
        account.setConfirmed(false);
        account.setConfirmationDeadline(null);
        account.setConfirmationKey(null);
        account.setResetDeadline(null);
        account.setResetKey(null);
        account.setChangeOnFirstAccess(false);

        account = accountRepository.save(account);
        return account;
    }

    public InternalUserAccount updateAccount(
            String subject,
            String realm,
            String userId,
            String email,
            String name,
            String surname,
            String lang) throws NoSuchUserException {

        InternalUserAccount account = getAccount(realm, userId);
        account.setSubject(subject);
        account.setEmail(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);

        account = accountRepository.saveAndFlush(account);
        return account;
    }

    public InternalUserAccount updatePassword(
            String subject,
            String realm,
            String username,
            String password,
            boolean changeOnFirstAccess) throws NoSuchUserException {

        InternalUserAccount account = getAccount(realm, username);
        // we expect password already hashed
        account.setPassword(password);
        account.setChangeOnFirstAccess(changeOnFirstAccess);

        account = accountRepository.saveAndFlush(account);
        return account;
    }

    public InternalUserAccount updateAccount(
            InternalUserAccount account) throws NoSuchUserException {
        if (account == null) {
            throw new NoSuchUserException();
        }
        account = accountRepository.saveAndFlush(account);
        return account;
    }

    public void deleteAccount(String subject,
            String realm,
            String username) {
        InternalUserAccount account = accountRepository.findByRealmAndUsername(realm, username);
        if (account != null) {
            accountRepository.delete(account);
        }

    }

}
