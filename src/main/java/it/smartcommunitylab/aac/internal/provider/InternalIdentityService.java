package it.smartcommunitylab.aac.internal.provider;

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
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.dto.LoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

public class InternalIdentityService
        extends AbstractInternalIdentityProvider<InternalUserAuthenticatedPrincipal, UserCredentials>
        implements IdentityService<InternalUserIdentity, InternalUserAccount>, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final InternalIdentityProviderConfig config;

    // services
    protected final UserEntityService userEntityService;

    // providers
    private final InternalAccountService accountService;
    private final InternalAuthenticationProvider authenticationProvider;

    public InternalIdentityService(
            String providerId,
            UserEntityService userEntityService, InternalUserAccountService userAccountService,
            SubjectService subjectService,
            InternalIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId,
                userEntityService, userAccountService, subjectService,
                config, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");

        // internal data repositories
        this.userEntityService = userEntityService;

        // config
        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountService = new InternalAccountService(providerId, userAccountService, subjectService, config,
                realm);
        this.authenticationProvider = new InternalAuthenticationProvider(providerId, userAccountService, accountService,
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
    public InternalIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public InternalAccountService getAccountService() {
        return accountService;
    }

    @Override
    public InternalAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
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
            account = accountService.createAccount(account);

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

//    @Override
//    public void deleteIdentity(String userId, String username) throws NoSuchUserException, RegistrationException {
//        // get the internal account entity
//        InternalUserAccount account = accountService.getAccount(username);
//        if (account != null) {
//            // check if userId matches account
//            if (!account.getUserId().equals(userId)) {
//                throw new RegistrationException("userid-mismatch");
//            }
//
//            // delete account via service
//            accountService.deleteAccount(username);
//        }
//    }

    @Override
    public String getRegistrationUrl() {
        // TODO filter
        // TODO build a realm-bound url, need updates on filters
        return "/auth/internal/register/" + getProvider();
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LoginProvider getLoginProvider() {
        // no direct login available
        return null;
    }

}
