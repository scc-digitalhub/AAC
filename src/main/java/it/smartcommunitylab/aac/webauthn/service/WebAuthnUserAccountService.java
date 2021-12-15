package it.smartcommunitylab.aac.webauthn.service;

import java.util.List;
import java.util.stream.Collectors;

import com.yubico.webauthn.data.ByteArray;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;

@Service
@Transactional
public class WebAuthnUserAccountService {

    // TODO: civts, try to Autowire this
    private final WebAuthnUserAccountRepository accountRepository;

    public WebAuthnUserAccountService(WebAuthnUserAccountRepository accountRepository) {
        Assert.notNull(accountRepository, "accountRepository is mandatory");
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount getAccount(long id) throws NoSuchUserException {
        WebAuthnUserAccount account = accountRepository.findOne(id);
        if (account == null) {
            throw new NoSuchUserException();
        }
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount getAccount(String userId) throws NoSuchUserException {
        WebAuthnUserAccount account = accountRepository.findBySubject(userId);
        if (account == null) {
            throw new NoSuchUserException();
        }
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount find(long id) {
        WebAuthnUserAccount account = accountRepository.findOne(id);
        if (account == null) {
            return null;
        }
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByUsername(String realm, String username) {
        WebAuthnUserAccount account = accountRepository.findByRealmAndUsername(realm, username);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserAccount> findBySubject(String subject, String realm) {
        List<WebAuthnUserAccount> accounts = accountRepository.findBySubjectAndRealm(subject, realm);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByEmail(String realm, String email) {
        WebAuthnUserAccount account = accountRepository.findByRealmAndEmailAddress(realm, email);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByCredentialId(ByteArray credentialId) {
        WebAuthnUserAccount account = accountRepository.findByCredentialCredentialId(credentialId);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    /*
     * CRUD
     */
    public WebAuthnUserAccount addAccount(
            WebAuthnUserAccount reg) throws RegistrationException {

        try {
            // we explode model
            WebAuthnUserAccount account = new WebAuthnUserAccount();

            account.setRealm(reg.getRealm());
            account.setUsername(reg.getUsername());
            account.setDisplayName(reg.getDisplayName());
            account.setCredential(reg.getCredential());
            account.setEmailAddress(reg.getEmailAddress());
            account.setSubject(reg.getSubject());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public WebAuthnUserAccount updateAccount(
            long id,
            WebAuthnUserAccount reg) throws NoSuchUserException, RegistrationException {
        if (reg == null) {
            throw new NoSuchUserException();
        }

        WebAuthnUserAccount account = accountRepository.findOne(id);
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // we explode model and update every field
            account.setRealm(reg.getRealm());
            account.setUsername(reg.getUsername());
            account.setDisplayName(reg.getDisplayName());
            account.setCredential(reg.getCredential());
            account.setEmailAddress(reg.getEmailAddress());
            account.setSubject(reg.getSubject());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public void deleteAccount(String userId) throws NoSuchUserException {
        WebAuthnUserAccount account = accountRepository.findBySubject(userId);
        if (account != null) {
            accountRepository.delete(account);
        } else {
            throw new NoSuchUserException();
        }
    }
}
