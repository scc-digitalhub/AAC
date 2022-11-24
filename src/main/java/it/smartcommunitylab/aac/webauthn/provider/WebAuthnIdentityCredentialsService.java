package it.smartcommunitylab.aac.webauthn.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserHandleService;

@Transactional
public class WebAuthnIdentityCredentialsService extends AbstractProvider<WebAuthnUserCredential> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();

    private final UserAccountService<InternalUserAccount> accountService;
    private final WebAuthnUserCredentialsService credentialsService;
    private final WebAuthnUserHandleService userHandleService;

    private final WebAuthnIdentityProviderConfig config;
    private final String repositoryId;

    public WebAuthnIdentityCredentialsService(String providerId,
            UserAccountService<InternalUserAccount> accountService, WebAuthnUserCredentialsService credentialsService,
            WebAuthnIdentityProviderConfig config, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(credentialsService, "webauthn credentials service is mandatory");
        Assert.notNull(config, "config is mandatory");

        this.config = config;

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

    public String getUuidFromUserHandle(String userHandle) throws NoSuchUserException {
        String username = userHandleService.getUsernameForUserHandle(repositoryId, userHandle);
        if (username == null) {
            throw new NoSuchUserException();
        }

        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        if (!StringUtils.hasText(account.getUuid())) {
            throw new NoSuchUserException();
        }

        return account.getUuid();

    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByUsername(String username) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // fetch all active credentials
        return credentialsService
                .findCredentialsByAccount(repositoryId, username).stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredential(String userHandle, String credentialId)
            throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountByUuid(getUuidFromUserHandle(userHandle));
        if (account == null) {
            throw new NoSuchUserException();
        }

        return credentialsService.findCredentialByUserHandleAndCredentialId(repositoryId, userHandle, credentialId);
    }

    public WebAuthnUserCredential updateCredentialCounter(String userHandle, String credentialId, long count)
            throws RegistrationException, NoSuchCredentialException {
        WebAuthnUserCredential c = credentialsService.findCredentialByUserHandleAndCredentialId(repositoryId,
                userHandle, credentialId);
        if (c == null) {
            throw new NoSuchCredentialException();
        }

        // allow only increment
        long signatureCount = c.getSignatureCount();
        if (count < signatureCount) {
            throw new InvalidDataException("signature-count");
        }

        // update field
        c.setSignatureCount(count);
        logger.debug("update credential {} signature count to {}", c.getCredentialId(), String.valueOf(count));

        c = credentialsService.updateCredentials(repositoryId, c.getId(), c);
        return c;
    }

    public void deleteCredentialsByUsername(String username) {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account != null) {
            // userHandle is uuid
            credentialsService.deleteCredentials(repositoryId, account.getUuid());
        }

    }
}