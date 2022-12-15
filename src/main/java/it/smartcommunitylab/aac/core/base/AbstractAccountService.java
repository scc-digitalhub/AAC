package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.model.PersistenceMode;
import it.smartcommunitylab.aac.model.SubjectStatus;

@Transactional
public abstract class AbstractAccountService<U extends AbstractAccount, E extends AbstractEditableAccount, M extends AbstractConfigMap, C extends AbstractAccountServiceConfig<M>>
        extends AbstractConfigurableProvider<U, ConfigurableAccountProvider, M, C>
        implements AccountService<U, E, M, C>, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final UserAccountService<U> userAccountService;
    protected ResourceEntityService resourceService;

    // provider configuration
    protected final C config;
    protected final String repositoryId;

    public AbstractAccountService(
            String authority, String providerId,
            UserAccountService<U> userAccountService,
            C config,
            String realm) {
        super(authority, providerId, realm, config);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.repositoryId = config.getRepositoryId();
        logger.debug("create {} account service for realm {} with id {} repository {}", String.valueOf(authority),
                String.valueOf(realm), String.valueOf(providerId));

        this.userAccountService = userAccountService;
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(resourceService, "resource service is mandatory");
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

//    @Override
//    @Transactional(readOnly = true)
//    public U findAccountByUuid(String uuid) {
//        U account = userAccountService.findAccountByUuid(uuid);
//        if (account == null) {
//            return null;
//        }
//
//        // check repository matches
//        if (!repositoryId.equals(account.getRepositoryId())) {
//            return null;
//        }
//
//        // map to our authority
//        account.setAuthority(getAuthority());
//        account.setProvider(getProvider());
//
//        return account;
//    }

    @Override
    @Transactional(readOnly = true)
    public U findAccount(String accountId) {
        U account = userAccountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public U getAccount(String accountId) throws NoSuchUserException {
        U account = findAccount(accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Override
    public void deleteAccount(String accountId) throws NoSuchUserException {
        U account = findAccount(accountId);

        if (account != null) {
            // remove account
            userAccountService.deleteAccount(repositoryId, accountId);
        }

        if (resourceService != null) {
            // remove resource
            resourceService.deleteResourceEntity(SystemKeys.RESOURCE_ACCOUNT, getAuthority(),
                    getProvider(), accountId);
        }
    }

    @Override
    public void deleteAccounts(String userId) {
        List<U> accounts = userAccountService.findAccountByUser(repositoryId, userId);
        for (U a : accounts) {
            // remove account
            userAccountService.deleteAccount(repositoryId, a.getAccountId());

            if (resourceService != null) {
                // remove resource
                resourceService.deleteResourceEntity(SystemKeys.RESOURCE_ACCOUNT, getAuthority(),
                        getProvider(), a.getAccountId());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<U> listAccounts(String userId) {
        List<U> accounts = userAccountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> {
            a.setAuthority(getAuthority());
            a.setProvider(getProvider());
        });
        return accounts;
    }

    @Override
    public U linkAccount(String accountId, String userId) throws NoSuchUserException, RegistrationException {
        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        U account = findAccount(accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // re-link to user
        account.setUserId(userId);
        account = userAccountService.updateAccount(repositoryId, accountId, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public U lockAccount(String accountId) throws NoSuchUserException, RegistrationException {
        return updateStatus(accountId, SubjectStatus.LOCKED);
    }

    @Override
    public U unlockAccount(String accountId) throws NoSuchUserException, RegistrationException {
        return updateStatus(accountId, SubjectStatus.ACTIVE);
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

        // check if attributes are persisted
        Map<String, Serializable> attributes = reg.getAttributes();
        if (PersistenceMode.REPOSITORY != config.getPersistence()) {
            // clear, not managed in any repository
            reg.setAttributes(null);
        }

        // TODO add validation?

        // create via service
        U account = userAccountService.addAccount(repositoryId, accountId, reg);
        accountId = account.getAccountId();

        logger.debug("user {} account {} created", String.valueOf(userId), String.valueOf(accountId));
        if (logger.isTraceEnabled()) {
            logger.trace("persisted account: {}", String.valueOf(account));
        }

        if (resourceService != null) {
            // register as user resource
            resourceService.addResourceEntity(account.getUuid(), SystemKeys.RESOURCE_ACCOUNT,
                    getAuthority(), getProvider(), account.getAccountId());
        }

        // check if attributes are persisted
        if (PersistenceMode.NONE != config.getPersistence()) {
            // set on result, only with NONE we leave these out
            account.setAttributes(attributes);
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

        // check if attributes are persisted
        Map<String, Serializable> attributes = reg.getAttributes();
        if (PersistenceMode.REPOSITORY != config.getPersistence()) {
            // clear, not managed in repository
            reg.setAttributes(null);
        }

        // TODO add validation?

        // update via service
        account = userAccountService.updateAccount(repositoryId, accountId, reg);

        logger.debug("user {} account {} updated", String.valueOf(userId), String.valueOf(accountId));
        if (logger.isTraceEnabled()) {
            logger.trace("persisted account: {}", String.valueOf(account));
        }

        // check if attributes are persisted
        if (PersistenceMode.NONE != config.getPersistence()) {
            // set on result, only with NONE we leave these out
            account.setAttributes(attributes);
        }

        return account;
    }

    @Override
    public E getEditableAccount(String userId, String accountId) throws NoSuchUserException {
        // not supported by default
        throw new UnsupportedOperationException();
    }

    @Override
    public E registerAccount(@Nullable String userId, EditableUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        // register is user-initiated, by default is not available
        throw new UnsupportedOperationException();
    }

    @Override
    public E editAccount(String userId, String accountId, EditableUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        // edit is user-initiated, by default is not available
        throw new UnsupportedOperationException();
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

    protected U updateStatus(String accountId, SubjectStatus newStatus)
            throws NoSuchUserException, RegistrationException {

        U account = findAccount(accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus && SubjectStatus.ACTIVE != newStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        logger.debug("update account {} status from {} to {}", accountId, curStatus, newStatus);

        // update status
        account.setStatus(newStatus.getValue());
        account = userAccountService.updateAccount(repositoryId, accountId, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

}
