package it.smartcommunitylab.aac.internal.provider;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;

@Transactional
public class InternalIdentityConfirmService extends AbstractProvider<UserCredentials> {

    private final InternalUserConfirmKeyService confirmKeyService;
    private final UserAccountService<InternalUserAccount> accountService;
    private final String repositoryId;

    public InternalIdentityConfirmService(String providerId,
            UserAccountService<InternalUserAccount> accountService,
            InternalUserConfirmKeyService confirmKeyService,
            InternalIdentityProviderConfig config, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm service is mandatory");
        Assert.notNull(config, "config is mandatory");

        this.accountService = accountService;
        this.confirmKeyService = confirmKeyService;

        // repositoryId from config
        this.repositoryId = config.getRepositoryId();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByConfirmationKey(String key) {
        InternalUserAccount account = confirmKeyService.findAccountByConfirmationKey(repositoryId, key);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    public InternalUserAccount confirmAccount(String username, String key)
            throws NoSuchUserException, RegistrationException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            return null;
        }

        account = confirmKeyService.confirmAccount(repositoryId, username, key);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

}
