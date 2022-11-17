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
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;
import it.smartcommunitylab.aac.core.provider.UserAccountService;

@Transactional
public abstract class AbstractAccountService<U extends AbstractAccount, E extends AbstractEditableAccount, M extends ConfigMap, C extends AccountServiceConfig<M>>
        extends AbstractConfigurableProvider<U, ConfigurableAccountProvider, M, C>
        implements AccountService<U, E, M, C>, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final UserAccountService<U> userAccountService;
    protected final String repositoryId;

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
        this.repositoryId = config.getRepositoryId();
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
    public U registerAccount(String userId, EditableUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        // register is user-initiated, by default is not available
        throw new UnsupportedOperationException();
    }

    @Override
    public U editAccount(String userId, String accountId, EditableUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        // edit is user-initiated, by default is not available
        throw new UnsupportedOperationException();
    }

    @Override
    public U createAccount(String userId, String accountId, UserAccount registration)
            throws NoSuchUserException, RegistrationException {
        // create is available for API,management etc
        logger.debug("create user {} account with id {}", String.valueOf(userId), String.valueOf(accountId));
        if (logger.isTraceEnabled()) {
            logger.trace("registration account {}", String.valueOf(registration));
        }

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // cast principal and handle errors
        U reg = null;
        try {
            @SuppressWarnings("unchecked")
            U u = (U) registration;
            reg = u;
        } catch (ClassCastException e) {
            logger.error("Wrong account class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported account");
        }

        // TODO add validation

        // create via service
        U account = userAccountService.addAccount(repositoryId, accountId, reg);
        accountId = account.getAccountId();

        logger.debug("user {} account {} created", String.valueOf(userId), String.valueOf(accountId));
        if (logger.isTraceEnabled()) {
            logger.trace("persisted account: {}", String.valueOf(account));
        }

        return account;
    }

    @Override
    public U updateAccount(String userId, String accountId, UserAccount registration)
            throws NoSuchUserException, RegistrationException {
        // update is available for API,management etc
        logger.debug("update user {} account with id {}", String.valueOf(userId), String.valueOf(accountId));

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // check if exists and match
        U account = userAccountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration account {}", String.valueOf(registration));
        }

        // cast principal and handle errors
        U reg = null;
        try {
            @SuppressWarnings("unchecked")
            U u = (U) registration;
            reg = u;
        } catch (ClassCastException e) {
            logger.error("Wrong account class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported account");
        }

        // TODO add validation

        // update via service
        account = userAccountService.updateAccount(repositoryId, accountId, reg);

        logger.debug("user {} account {} updated", String.valueOf(userId), String.valueOf(accountId));
        if (logger.isTraceEnabled()) {
            logger.trace("persisted account: {}", String.valueOf(account));
        }

        return account;
    }

    @Override
    public U verifyAccount(String accountId) throws NoSuchUserException, RegistrationException {
        // verification is provider-specific, by default is not available
        throw new UnsupportedOperationException();
    }

    @Override
    public U confirmAccount(String accountId) throws NoSuchUserException, RegistrationException {
        // verification is provider-specific, by default is not available
        throw new UnsupportedOperationException();
    }

    @Override
    public U unconfirmAccount(String accountId) throws NoSuchUserException, RegistrationException {
        // verification is provider-specific, by default is not available
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRegistrationUrl() {
        // register is user-initiated, by default is not available
        return null;
    }
}
