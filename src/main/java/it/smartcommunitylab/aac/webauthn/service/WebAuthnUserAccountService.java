package it.smartcommunitylab.aac.webauthn.service;

import com.yubico.webauthn.data.ByteArray;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;

@Service
@Transactional
public class WebAuthnUserAccountService {

    private final WebAuthnUserAccountRepository accountRepository;

    private final WebAuthnCredentialsRepository credentialRepository;

    public WebAuthnUserAccountService(WebAuthnUserAccountRepository accountRepository,
            WebAuthnCredentialsRepository credentialRepository) {
        Assert.notNull(accountRepository, "accountRepository is mandatory");
        Assert.notNull(credentialRepository, "credentialRepository is mandatory");
        this.accountRepository = accountRepository;
        this.credentialRepository = credentialRepository;
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
    public WebAuthnUserAccount findByRealmAndUsername(String realm, String username) {
        WebAuthnUserAccount account = accountRepository.findByRealmAndUsername(realm, username);
        if (account == null) {
            return null;
        }
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findBySubject(String subject, String realm) {
        WebAuthnUserAccount account = accountRepository.findBySubjectAndRealm(subject, realm);
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByEmail(String realm, String email) {
        WebAuthnUserAccount account = accountRepository.findByRealmAndEmailAddress(realm, email);
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByCredentialId(ByteArray credentialId) {
        WebAuthnCredential cred = credentialRepository.findByCredentialId(credentialId.getBase64());
        if (cred == null) {
            return null;
        }
        return accountRepository.detach(cred.getParentAccount());
    }

    /*
     * CRUD
     */
    public WebAuthnUserAccount addAccount(
            WebAuthnUserAccount reg) throws RegistrationException {

        try {
            // we explode model
            WebAuthnUserAccount account = new WebAuthnUserAccount();

            account.setCredentials(reg.getCredentials());
            account.setEmailAddress(reg.getEmailAddress());
            account.setProvider(reg.getProvider());
            account.setRealm(reg.getRealm());
            account.setUserHandle(reg.getUserHandle());
            account.setUsername(reg.getUsername());

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
            account.setCredentials(reg.getCredentials());
            account.setEmailAddress(reg.getEmailAddress());
            account.setProvider(reg.getProvider());
            account.setRealm(reg.getRealm());
            account.setUserHandle(reg.getUserHandle());
            account.setUsername(reg.getUsername());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public void deleteAccount(String subject) throws NoSuchUserException {
        WebAuthnUserAccount account = accountRepository.findBySubject(subject);
        if (account != null) {
            accountRepository.delete(account);
        } else {
            throw new NoSuchUserException();
        }
    }
}
