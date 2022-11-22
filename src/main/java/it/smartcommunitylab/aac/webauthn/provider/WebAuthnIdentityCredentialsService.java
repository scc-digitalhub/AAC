package it.smartcommunitylab.aac.webauthn.provider;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnCredentialsService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserHandleService;

@Transactional
public class WebAuthnIdentityCredentialsService extends AbstractProvider<WebAuthnUserCredential> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<InternalUserAccount> accountService;
    private final WebAuthnCredentialsService credentialsService;
    private final WebAuthnUserHandleService userHandleService;
    private final String repositoryId;

    public WebAuthnIdentityCredentialsService(String providerId,
            UserAccountService<InternalUserAccount> accountService, WebAuthnCredentialsService credentialsService,
            WebAuthnIdentityProviderConfig config, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(credentialsService, "webauthn credentials service is mandatory");
        Assert.notNull(config, "config is mandatory");

        this.accountService = accountService;
        this.credentialsService = credentialsService;

        // repositoryId from config
        this.repositoryId = config.getRepositoryId();

        // build service
        this.userHandleService = new WebAuthnUserHandleService(accountService);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    public String getUuidFromUserHandle(String userHandle) {
        String username = userHandleService.getUsernameForUserHandle(repositoryId, userHandle);
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            return null;
        }

        if (!StringUtils.hasText(account.getUuid())) {
            return null;
        }

        return account.getUuid();

    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByUsername(String username) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return credentialsService.findActiveCredentialsByUsername(repositoryId, username);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredential(String userHandle, String credentialId)
            throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountByUuid(repositoryId, getUuidFromUserHandle(userHandle));
        if (account == null) {
            throw new NoSuchUserException();
        }

        return credentialsService.findCredentialByUserHandleAndCredentialId(repositoryId, userHandle, credentialId);
    }

    public WebAuthnUserCredential updateCredentialCounter(String userHandle, String credentialId, long signatureCount)
            throws RegistrationException, NoSuchCredentialException {
        return credentialsService.updateCredentialCounter(repositoryId, userHandle, credentialId, signatureCount);
    }

    public void deleteCredentialsByUsername(String username) {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account != null) {
            // userHandle is uuid
            credentialsService.deleteCredentials(repositoryId, account.getUuid());
        }

    }
}