package it.smartcommunitylab.aac.internal.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.utils.MailService;

public class InternalIdentityService
        extends
        AbstractConfigurableProvider<InternalUserIdentity, ConfigurableIdentityService, InternalIdentityServiceConfigMap, InternalIdentityServiceConfig>
        implements
        IdentityService<InternalUserIdentity, InternalUserAccount, InternalIdentityServiceConfigMap, InternalIdentityServiceConfig>,
        InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final InternalIdentityServiceConfig config;

    // services
    protected final UserEntityService userEntityService;

    // providers
    private final InternalAccountService accountService;

    public InternalIdentityService(
            String providerId,
            UserEntityService userEntityService,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            InternalIdentityServiceConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId,
                realm, providerConfig);
        Assert.notNull(userEntityService, "user entity service is mandatory");
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(confirmKeyService, "user confirm service is mandatory");

        // internal data repositories
        this.userEntityService = userEntityService;

        // config
        this.config = providerConfig;

        // build resource providers, we use our providerId to ensure consistency
        this.accountService = new InternalAccountService(providerId, userAccountService, confirmKeyService,
                config, realm);

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(accountService, "account service is mandatory");
    }

    public void setMailService(MailService mailService) {
        // assign to services
        this.accountService.setMailService(mailService);
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        // assign to services
        this.accountService.setUriBuilder(uriBuilder);
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public InternalAccountService getAccountService() {
        return accountService;
    }

    protected InternalUserIdentity buildIdentity(InternalUserAccount account,
            InternalUserAuthenticatedPrincipal principal,
            Collection<UserAttributes> attributes) {
        // build identity
        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account,
                principal);
        identity.setAttributes(attributes);

        return identity;
    }

    @Override
    public InternalUserIdentity findIdentity(String userId, String username) {
        logger.debug("find identity for id {} user {}", String.valueOf(username), String.valueOf(userId));

        // lookup a matching account
        InternalUserAccount account = accountService.findAccount(username);
        if (account == null) {
            return null;
        }

        // check userId matches
        if (!account.getUserId().equals(userId)) {
            return null;
        }

        // build identity without attributes or principal
        InternalUserIdentity identity = buildIdentity(account, null, null);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    public InternalUserIdentity getIdentity(String userId, String username) throws NoSuchUserException {
        logger.debug("get identity for id {} user {}", String.valueOf(username), String.valueOf(userId));

        // lookup a matching account
        InternalUserAccount account = accountService.getAccount(username);

        // check userId matches
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user mismatch");
        }

        // build identity without attributes or principal
        InternalUserIdentity identity = buildIdentity(account, null, null);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    public Collection<InternalUserIdentity> listIdentities(String userId) {
        logger.debug("list identities for user {}", String.valueOf(userId));
        // lookup for matching accounts
        Collection<InternalUserAccount> accounts = accountService.listAccounts(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<InternalUserIdentity> identities = new ArrayList<>();
        for (InternalUserAccount account : accounts) {

            InternalUserIdentity identity = buildIdentity(account, null, null);
            if (logger.isTraceEnabled()) {
                logger.trace("identity: {}", String.valueOf(identity));
            }

            identities.add(identity);
        }

        return identities;
    }

    @Override
    public InternalUserIdentity registerIdentity(
            String userId, UserIdentity registration)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("registration is disabled for this provider");
        }

        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserIdentity.class, registration,
                "registration must be an instance of internal user identity");
        InternalUserIdentity reg = (InternalUserIdentity) registration;

        // check email for confirmation when required
        if (config.isConfirmationRequired()) {
            if (reg.getEmailAddress() == null) {
                throw new MissingDataException("email");
            }

            String email = Jsoup.clean(reg.getEmailAddress(), Safelist.none());
            if (!StringUtils.hasText(email)) {
                throw new MissingDataException("email");
            }
        }

        // registration is create but user-initiated
        InternalUserIdentity identity = createIdentity(userId, registration);
        InternalUserAccount account = identity.getAccount();
        String username = account.getUsername();

        if (config.isConfirmationRequired() && !account.isConfirmed()) {
            account = accountService.verifyAccount(username);
        }

        return identity;
    }

    @Override
    public InternalUserIdentity createIdentity(
            String userId, UserIdentity registration)
            throws NoSuchUserException, RegistrationException {

        // create is always enabled
        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserIdentity.class, registration,
                "registration must be an instance of internal user identity");
        InternalUserIdentity reg = (InternalUserIdentity) registration;

        // check for account details
        InternalUserAccount account = reg.getAccount();
        if (account == null) {
            throw new MissingDataException("account");
        }

        // validate base param, nothing to do when missing
        String username = reg.getUsername();
        if (StringUtils.hasText(username)) {
            username = Jsoup.clean(username, Safelist.none());
        }
        if (!StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }
        String emailAddress = reg.getEmailAddress();

        // no additional attributes supported

        // we expect subject to be valid, or null if we need to create
        UserEntity user = null;
        if (!StringUtils.hasText(userId)) {
            String realm = getRealm();

            userId = userEntityService.createUser(realm).getUuid();
            user = userEntityService.addUser(userId, realm, username, emailAddress);
            userId = user.getUuid();
        } else {
            // check if exists
            userEntityService.getUser(userId);
        }

        // make sure userId is correct
        account.setUserId(userId);

        try {
            // create internal account
            account = accountService.createAccount(username, account);

            // store and update attributes
            // we shouldn't have additional attributes for internal

            // use builder to properly map attributes
            InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(),
                    account);

            // no attribute sets
            // this identity has no credentials
            return identity;
        } catch (RegistrationException | IllegalArgumentException e) {
            // cleanup subject if we created it
            if (user != null) {
                userEntityService.deleteUser(userId);
            }

            throw e;
        }
    }

    @Override
    public InternalUserIdentity updateIdentity(
            String userId,
            String username, UserIdentity registration)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserIdentity.class, registration,
                "registration must be an instance of internal user identity");
        InternalUserIdentity reg = (InternalUserIdentity) registration;
        if (reg.getAccount() == null) {
            throw new MissingDataException("account");
        }

        // get the internal account entity
        InternalUserAccount account = accountService.getAccount(username);

        // check if userId matches account
        if (!account.getUserId().equals(userId)) {
            throw new RegistrationException("userid-mismatch");
        }

        // update account
        account = accountService.updateAccount(username, reg.getAccount());

        // store and update attributes
        // we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account);

        // no attribute sets
        return identity;
    }

    @Override
    public void deleteIdentities(String userId) {
        logger.debug("delete identities for user {}", String.valueOf(userId));
        Collection<InternalUserAccount> accounts = accountService.listAccounts(userId);
        for (InternalUserAccount account : accounts) {
            try {
                deleteIdentity(userId, account.getAccountId());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public void deleteIdentity(String userId, String username) throws NoSuchUserException, RegistrationException {
        logger.debug("delete identity with id {} for user {}", String.valueOf(username), String.valueOf(userId));

        // get the internal account entity
        InternalUserAccount account = accountService.getAccount(username);
        if (account != null) {
            // check if userId matches account
            if (!account.getUserId().equals(userId)) {
                throw new RegistrationException("userid-mismatch");
            }

            // delete account via service
            accountService.deleteAccount(username);
        }
    }

    @Override
    public String getRegistrationUrl() {
        // TODO filter
        // TODO build a realm-bound url, need updates on filters
        return "/auth/internal/register/" + getProvider();
    }

}
