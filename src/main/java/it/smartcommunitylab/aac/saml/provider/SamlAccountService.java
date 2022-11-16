package it.smartcommunitylab.aac.saml.provider;

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
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

@Transactional
public class SamlAccountService extends SamlAccountProvider implements
        AccountService<SamlUserAccount, SamlIdentityProviderConfigMap, SamlAccountServiceConfig> {

    private final SamlAccountServiceConfig config;

    public SamlAccountService(String providerId,
            UserAccountService<SamlUserAccount> accountService,
            SamlAccountServiceConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_SAML, providerId, accountService, config, realm);
    }

    public SamlAccountService(String authority, String providerId,
            UserAccountService<SamlUserAccount> accountService,
            SamlAccountServiceConfig config,
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
    public SamlAccountServiceConfig getConfig() {
        return config;
    }

    @Override
    public ConfigurableAccountProvider getConfigurable() {
        return null;
    }

    @Override
    public SamlUserAccount registerAccount(String userId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SamlUserAccount createAccount(String userId, String accountId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SamlUserAccount updateAccount(String userId, String accountId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SamlUserAccount verifyAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SamlUserAccount confirmAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SamlUserAccount unconfirmAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRegistrationUrl() {
        return null;
    }
}
