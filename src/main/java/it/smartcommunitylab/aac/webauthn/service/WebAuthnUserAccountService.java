package it.smartcommunitylab.aac.webauthn.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;

import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialId;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountId;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;

@Service
@Transactional
public class WebAuthnUserAccountService {

    @Autowired
    private WebAuthnUserAccountRepository accountRepository;

    @Autowired
    private WebAuthnCredentialsRepository credentialsRepository;

    /*
     * User accounts
     */

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByUsername(String provider, String username) {
        WebAuthnUserAccount account = accountRepository.findOne(new WebAuthnUserAccountId(provider, username));
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserAccount> findAccountByEmailAddress(String provider, String email) {
        List<WebAuthnUserAccount> accounts = accountRepository.findByProviderAndEmailAddress(provider, email);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByConfirmationKey(String provider, String key) {
        WebAuthnUserAccount account = accountRepository.findByProviderAndConfirmationKey(provider, key);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByUserHandle(String provider, String userHandle) {
        WebAuthnUserAccount account = accountRepository.findByProviderAndUserHandle(provider, userHandle);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByUuid(String provider, String uuid) {
        WebAuthnUserAccount account = accountRepository.findByProviderAndUuid(provider, uuid);
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserAccount> listAccountsByUser(String userId) {
        List<WebAuthnUserAccount> accounts = accountRepository.findByUserId(userId);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserAccount> listAccountsByUser(String userId, String provider) {
        List<WebAuthnUserAccount> accounts = accountRepository.findByUserIdAndProvider(userId, provider);
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByCredentialId(String provider, String credentialId) {
        WebAuthnCredential cred = credentialsRepository.findOne(new WebAuthnCredentialId(provider, credentialId));
        if (cred == null) {
            return null;
        }

        WebAuthnUserAccount account = accountRepository.findByProviderAndUserHandle(provider, cred.getUserHandle());
        if (account == null) {
            return null;
        }

        return accountRepository.detach(account);
    }

    public WebAuthnUserAccount addAccount(
            String provider,
            WebAuthnUserAccount reg) throws RegistrationException {

        try {
            // we explode model
            WebAuthnUserAccount account = new WebAuthnUserAccount();
            account.setProvider(provider);
            account.setUsername(reg.getUsername());
            account.setUserHandle(reg.getUserHandle());

            account.setUuid(reg.getUuid());
            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());

            account.setStatus(reg.getStatus());
            account.setEmailAddress(reg.getEmailAddress());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getLang());

            account.setConfirmed(reg.isConfirmed());
            account.setConfirmationDeadline(reg.getConfirmationDeadline());
            account.setConfirmationKey(reg.getConfirmationKey());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            return account;
        } catch (RegistrationException e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public WebAuthnUserAccount updateAccount(
            String provider, String userHandle,
            WebAuthnUserAccount reg) throws NoSuchUserException, RegistrationException {
        if (reg == null) {
            throw new NoSuchUserException();
        }

        WebAuthnUserAccount account = accountRepository.findByProviderAndUserHandle(provider, userHandle);
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // we explode model and update every attribute field
            account.setUuid(reg.getUuid());
            account.setUserId(reg.getUserId());
            account.setRealm(reg.getRealm());

            account.setEmailAddress(reg.getEmailAddress());
            account.setUsername(reg.getUsername());
            account.setName(reg.getName());
            account.setSurname(reg.getSurname());
            account.setLang(reg.getLang());

            account.setConfirmed(reg.isConfirmed());
            account.setConfirmationDeadline(reg.getConfirmationDeadline());
            account.setConfirmationKey(reg.getConfirmationKey());

            account.setStatus(reg.getStatus());

            account = accountRepository.saveAndFlush(account);
            account = accountRepository.detach(account);

            return account;
        } catch (RegistrationException e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    public void deleteAccount(String provider, String userHandle) {
        WebAuthnUserAccount account = accountRepository.findByProviderAndUserHandle(provider, userHandle);
        List<WebAuthnCredential> credentials = credentialsRepository.findByProviderAndUserHandle(provider, userHandle);
        if (credentials != null && !credentials.isEmpty()) {
            credentialsRepository.deleteAll(credentials);
        }

        if (account != null) {
            accountRepository.delete(account);
        }
    }

    /*
     * WebAuthn Credentials
     */

    public WebAuthnCredential findCredentialById(String provider, String credentialId) {
        WebAuthnCredential c = credentialsRepository.findOne(new WebAuthnCredentialId(provider, credentialId));
        if (c == null) {
            return null;
        }
        return credentialsRepository.detach(c);
    }

    public List<WebAuthnCredential> findCredentialsByUserHandle(String provider, String userHandle) {
        return credentialsRepository.findByProviderAndUserHandle(provider, userHandle);
    }

    public WebAuthnCredential findByCredentialByUserHandleAndId(String provider, String userHandle,
            String credentialId) {
        return credentialsRepository.findByProviderAndUserHandleAndCredentialId(provider, userHandle, credentialId);
    }

    public WebAuthnCredential addCredential(String provider, WebAuthnCredential reg)
            throws NoSuchUserException, RegistrationException {
        // validate credentials
        if (!StringUtils.hasText(reg.getCredentialId())) {
            throw new IllegalArgumentException();
        }
        if (!StringUtils.hasText(reg.getUserHandle())) {
            throw new IllegalArgumentException();
        }
        if (!StringUtils.hasText(reg.getPublicKeyCose())) {
            throw new IllegalArgumentException();
        }

        // check duplicate
        WebAuthnCredential c = credentialsRepository.findOne(new WebAuthnCredentialId(provider, reg.getCredentialId()));
        if (c != null) {
            throw new RegistrationException("duplicate-registration");
        }

        // validate user
        WebAuthnUserAccount account = accountRepository.findByProviderAndUserHandle(provider,
                reg.getUserHandle());
        if (account == null) {
            throw new NoSuchUserException();
        }

        // rebuild model
        c = new WebAuthnCredential();
        c.setProvider(provider);
        c.setCredentialId(reg.getCredentialId());
        c.setUserHandle(reg.getUserHandle());

        c.setDisplayName(reg.getDisplayName());
        c.setPublicKeyCose(reg.getPublicKeyCose());
        c.setTransports(reg.getTransports());
        c.setSignatureCount(0);

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public WebAuthnCredential updateCredential(String provider, String credentialId, WebAuthnCredential reg)
            throws NoSuchUserException, RegistrationException {
        WebAuthnCredential c = credentialsRepository.findOne(new WebAuthnCredentialId(provider, credentialId));
        if (c == null) {
            throw new NoSuchUserException();
        }

        // update allowed fields
        c.setDisplayName(reg.getDisplayName());
        c.setSignatureCount(reg.getSignatureCount());

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public WebAuthnCredential updateCredentialCounter(String provider, String credentialId, long count)
            throws NoSuchUserException, RegistrationException {
        WebAuthnCredential c = credentialsRepository.findOne(new WebAuthnCredentialId(provider, credentialId));
        if (c == null) {
            throw new NoSuchUserException();
        }

        // update field
        c.setSignatureCount(count);

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public void deleteCredential(String provider, String credentialId) {
        WebAuthnCredential c = credentialsRepository.findOne(new WebAuthnCredentialId(provider, credentialId));
        if (c != null) {
            credentialsRepository.delete(c);
        }
    }

}
