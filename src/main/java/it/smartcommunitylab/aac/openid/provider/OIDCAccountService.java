package it.smartcommunitylab.aac.openid.provider;

import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Transactional
public class OIDCAccountService extends OIDCAccountProvider implements
        AccountService<OIDCUserAccount, OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig> {

    private final OIDCAccountServiceConfig config;

    public OIDCAccountService(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCAccountServiceConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, config, realm);
    }

    public OIDCAccountService(String authority, String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCAccountServiceConfig config,
            String realm) {
        super(authority, providerId, accountService, config.getRepositoryId(), realm);
        Assert.notNull(config, "config can not be null");
        this.config = config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getTitle(Locale locale) {
        return config.getTitle(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return config.getDescription(locale);
    }

    @Override
    public OIDCAccountServiceConfig getConfig() {
        return config;
    }

    @Override
    public ConfigurableAccountProvider getConfigurable() {
        return null;
    }

    @Override
    public OIDCUserAccount registerAccount(String userId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OIDCUserAccount createAccount(String userId, String accountId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OIDCUserAccount updateAccount(String userId, String accountId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OIDCUserAccount verifyAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OIDCUserAccount confirmAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OIDCUserAccount unconfirmAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRegistrationUrl() {
        return null;
    }
}
