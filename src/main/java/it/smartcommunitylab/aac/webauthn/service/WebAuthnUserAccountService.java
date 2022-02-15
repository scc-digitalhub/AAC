package it.smartcommunitylab.aac.webauthn.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;

@Service
@Transactional
public class WebAuthnUserAccountService {

    @Autowired
    private  WebAuthnUserAccountRepository accountRepository;

    @Autowired
    private  WebAuthnCredentialsRepository credentialRepository;
 

    @Transactional(readOnly = true)
    public List<WebAuthnUserAccount> findBySubjectAndRealm(String subject, String realm) throws NoSuchUserException {
        List<WebAuthnUserAccount> accounts = accountRepository.findBySubjectAndRealm(subject, realm);
        return accounts.stream().map(a -> accountRepository.detach(a)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByProviderAndUsername(String provider, String username) {
        WebAuthnUserAccount account = accountRepository.findByProviderAndUsername(provider, username);
        if (account == null) {
            return null;
        }
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByProviderAndSubject(String provider, String subject) throws NoSuchUserException {
        WebAuthnUserAccount account = accountRepository.findByProviderAndSubject(provider, subject);
        if (account == null) {
            throw new NoSuchUserException();
        }
        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByUserHandle(String userHandle) {
        WebAuthnUserAccount account = accountRepository.findByUserHandle(userHandle);
        if (account == null) {
            return null;
        }
        return accountRepository.detach(account);
    }
 
    @Transactional(readOnly = true)
    public WebAuthnUserAccount findByCredentialId(String credentialId) {
        WebAuthnCredential cred = credentialRepository.findByCredentialId(credentialId);
        if (cred == null) {
            return null;
        }
        WebAuthnUserAccount userAccount = accountRepository.getOne(cred.getUserHandle());
        return accountRepository.detach(userAccount);
    }

    /*
     * CRUD
     */
    public WebAuthnUserAccount addAccount(
            WebAuthnUserAccount reg) throws RegistrationException {

        try {
            // we explode model
            WebAuthnUserAccount account = new WebAuthnUserAccount();

            account.setEmailAddress(reg.getEmailAddress());
            account.setProvider(reg.getProvider());
            account.setRealm(reg.getRealm());
            account.setUserHandle(reg.getUserHandle());
            account.setUsername(reg.getUsername());
            account.setSubject(reg.getSubject());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            return account;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public WebAuthnUserAccount updateAccount(
            String userHandle,
            WebAuthnUserAccount reg) throws NoSuchUserException, RegistrationException {
        if (reg == null) {
            throw new NoSuchUserException();
        }

        WebAuthnUserAccount account = accountRepository.findByUserHandle(userHandle);
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // we explode model and update every field
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

    public void deleteAccount(String provider, String subject) {
        final WebAuthnUserAccount account = accountRepository.findByProviderAndSubject(provider, subject);
        for (final WebAuthnCredential c : credentialRepository.findByUserHandle(account.getUserHandle())) {
            credentialRepository.delete(c);
        }
        accountRepository.delete(account);
    }

}
