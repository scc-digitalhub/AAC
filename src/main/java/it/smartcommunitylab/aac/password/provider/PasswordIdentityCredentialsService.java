package it.smartcommunitylab.aac.password.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.service.InternalUserPasswordService;

@Transactional
public class PasswordIdentityCredentialsService extends AbstractProvider<InternalUserPassword> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PasswordIdentityProviderConfig config;

    private final InternalUserPasswordService passwordService;
    private final UserAccountService<InternalUserAccount> accountService;
    private final String repositoryId;

    public PasswordIdentityCredentialsService(String providerId,
            UserAccountService<InternalUserAccount> accountService, InternalUserPasswordService passwordService,
            PasswordIdentityProviderConfig config, String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");
        Assert.notNull(config, "config is mandatory");

        this.config = config;

        this.accountService = accountService;
        this.passwordService = passwordService;

        // repositoryId from config
        this.repositoryId = config.getRepositoryId();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    @Transactional(readOnly = true)
    public InternalUserPassword findPassword(String username) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return passwordService.findPassword(repositoryId, username);
    }

    public boolean verifyPassword(String username, String password) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return passwordService.verifyPassword(repositoryId, username, password);
    }

    public boolean confirmReset(String resetKey) throws NoSuchCredentialException {
        InternalUserPassword pass = passwordService.confirmReset(repositoryId, resetKey);
        return pass != null;
    }

    public void deletePassword(String username) throws NoSuchUserException {
        passwordService.deletePassword(repositoryId, username);
    }

}
