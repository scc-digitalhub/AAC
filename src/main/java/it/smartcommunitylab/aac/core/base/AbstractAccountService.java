package it.smartcommunitylab.aac.core.base;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;
import it.smartcommunitylab.aac.core.provider.UserAccountService;

@Transactional
public abstract class AbstractAccountService<U extends UserAccount, M extends ConfigMap, C extends AccountServiceConfig<M>>
        extends AbstractConfigurableProvider<U, ConfigurableAccountProvider, M, C>
        implements AccountService<U, M, C>, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final UserAccountService<U> userAccountService;

    public AbstractAccountService(
            String authority, String providerId,
            UserAccountService<U> userAccountService,
            C config,
            String realm) {
        super(authority, providerId, realm, config);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        Assert.isTrue(authority.equals(config.getAuthority()),
                "configuration does not match this provider");
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        logger.debug("create {} account service for realm {} with id {}", String.valueOf(authority),
                String.valueOf(realm), String.valueOf(providerId));

        this.userAccountService = userAccountService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(getAccountProvider(), "account provider is mandatory");
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    protected abstract AccountProvider<U> getAccountProvider();

    /*
     * Account provider methods: delegated
     */
    public U findAccountByUuid(String uuid) {
        return getAccountProvider().findAccountByUuid(uuid);
    }

    public U findAccount(String accountId) {
        return getAccountProvider().findAccount(accountId);
    }

    public U getAccount(String accountId) throws NoSuchUserException {
        return getAccountProvider().getAccount(accountId);
    }

    public void deleteAccount(String accountId) throws NoSuchUserException {
        getAccountProvider().deleteAccount(accountId);
    }

    public void deleteAccounts(String userId) {
        getAccountProvider().deleteAccounts(userId);
    }

    public Collection<U> listAccounts(String userId) {
        return getAccountProvider().listAccounts(userId);
    }

    public U linkAccount(String accountId, String userId) throws NoSuchUserException, RegistrationException {
        return getAccountProvider().linkAccount(accountId, userId);
    }

    public U lockAccount(String accountId) throws NoSuchUserException, RegistrationException {
        return getAccountProvider().lockAccount(accountId);
    }

    public U unlockAccount(String accountId) throws NoSuchUserException, RegistrationException {
        return getAccountProvider().unlockAccount(accountId);
    }

    /*
     * Account service: not implemented by default
     */

    @Override
    public U registerAccount(String userId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public U createAccount(String userId, String accountId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public U updateAccount(String userId, String accountId, UserAccount account)
            throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public U verifyAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public U confirmAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public U unconfirmAccount(String accountId) throws NoSuchUserException, RegistrationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRegistrationUrl() {
        return null;
    }
}
