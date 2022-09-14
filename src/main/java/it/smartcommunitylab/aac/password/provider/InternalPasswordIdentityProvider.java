package it.smartcommunitylab.aac.password.provider;

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.IdentityCredentialsProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalSubjectResolver;
import it.smartcommunitylab.aac.password.InternalPasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.model.InternalPasswordUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.service.InternalPasswordService;
import it.smartcommunitylab.aac.utils.MailService;

public class InternalPasswordIdentityProvider extends
        AbstractIdentityProvider<InternalUserIdentity, InternalUserAccount, InternalPasswordUserAuthenticatedPrincipal, InternalPasswordIdentityProviderConfigMap, InternalPasswordIdentityProviderConfig>
        implements IdentityCredentialsProvider<InternalUserAccount, InternalUserPassword> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final InternalPasswordIdentityProviderConfig config;

    // services
    private final UserAccountService<InternalUserAccount> userAccountService;
    private final InternalPasswordService passwordService;

    // providers
    private final InternalPasswordAuthenticationProvider authenticationProvider;
    protected final InternalAccountProvider accountProvider;
    protected final InternalAttributeProvider<InternalPasswordUserAuthenticatedPrincipal> attributeProvider;
    protected final SubjectResolver<InternalUserAccount> subjectResolver;

    public InternalPasswordIdentityProvider(
            String providerId,
            UserEntityService userEntityService, UserAccountService<InternalUserAccount> userAccountService,
            SubjectService subjectService,
            InternalUserPasswordRepository passwordRepository,
            InternalPasswordIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId,
                userEntityService, userAccountService, subjectService,
                config, realm);
        Assert.notNull(passwordRepository, "password repository is mandatory");

        logger.debug("create password provider with id {}", String.valueOf(providerId));
        this.config = config;

        InternalIdentityProviderConfig internalConfig = config.toInternalProviderConfig();

        // internal data repositories
        this.userAccountService = userAccountService;

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new InternalAttributeProvider<>(SystemKeys.AUTHORITY_PASSWORD, providerId,
                internalConfig, realm);
        this.accountProvider = new InternalAccountProvider(SystemKeys.AUTHORITY_PASSWORD, providerId,
                userAccountService, internalConfig, realm);

        // build providers
        this.passwordService = new InternalPasswordService(providerId, userAccountService, passwordRepository, config,
                realm);
        this.authenticationProvider = new InternalPasswordAuthenticationProvider(providerId, userAccountService,
                accountProvider, passwordService, config, realm);

        // always expose a valid resolver to satisfy authenticationManager at post login
        // TODO refactor to avoid fetching via resolver at this stage
        this.subjectResolver = new InternalSubjectResolver(providerId, userAccountService, internalConfig, realm);

    }

    public void setMailService(MailService mailService) {
        this.passwordService.setMailService(mailService);
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.passwordService.setUriBuilder(uriBuilder);
    }

    @Override
    public boolean isAuthoritative() {
        // password handles only login
        return false;
    }

    public CredentialsType getCredentialsType() {
        return CredentialsType.PASSWORD;
    }

    @Override
    public InternalPasswordIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public InternalPasswordAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public InternalPasswordService getCredentialsService() {
        return passwordService;
    }

    @Override
    public InternalAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public InternalAttributeProvider<InternalPasswordUserAuthenticatedPrincipal> getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public SubjectResolver<InternalUserAccount> getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    protected InternalUserIdentity buildIdentity(InternalUserAccount account,
            InternalPasswordUserAuthenticatedPrincipal principal,
            Collection<UserAttributes> attributes) {
        // build identity
        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account,
                principal);
        identity.setAttributes(attributes);

        // if attributes then load credentials
        if (attributes != null) {
            InternalUserPassword password = passwordService.findPassword(account.getUsername());
            if (password != null) {
                password.eraseCredentials();
                identity.setCredentials(Collections.singletonList(password));
            }
        }

        return identity;
    }

    // TODO remove and set accountProvider read-only (and let create fail in super)
    @Override
    @Transactional(readOnly = false)
    public InternalUserIdentity convertIdentity(UserAuthenticatedPrincipal authPrincipal, String userId)
            throws NoSuchUserException {
        Assert.isInstanceOf(InternalPasswordUserAuthenticatedPrincipal.class, authPrincipal, "Wrong principal class");
        logger.debug("convert principal to identity for user {}", String.valueOf(userId));
        if (logger.isTraceEnabled()) {
            logger.trace("principal {}", String.valueOf(authPrincipal));
        }

        InternalPasswordUserAuthenticatedPrincipal principal = (InternalPasswordUserAuthenticatedPrincipal) authPrincipal;

        // username binds all identity pieces together
        String username = principal.getUsername();

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // get the internal account entity
        InternalUserAccount account = accountProvider.findAccount(username);

        if (account == null) {
            // error, user should already exists for authentication
            throw new NoSuchUserException();
        }

        // uuid is available for persisted accounts
        String uuid = account.getUuid();
        principal.setUuid(uuid);

        // userId is always present, is derived from the same account table
        String curUserId = account.getUserId();

        if (!curUserId.equals(userId)) {
//            // force link
//            // TODO re-evaluate
//            account.setSubject(subjectId);
//            account = accountRepository.save(account);
            throw new IllegalArgumentException("user mismatch");
        }

        // store and update attributes
        // we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account,
                principal);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal,
                account);
        identity.setAttributes(identityAttributes);

        return identity;
    }

//    /*
//     * User registration
//     * 
//     * TODO remove and handle via internalIdentityService
//     */
//
//    public InternalUserIdentity registerIdentity(
//            String userId, UserIdentity registration, UserCredentials credentials)
//            throws NoSuchUserException, RegistrationException {
//        if (!config.isEnableRegistration()) {
//            throw new IllegalArgumentException("registration is disabled for this provider");
//        }
//
//        if (registration == null) {
//            throw new RegistrationException();
//        }
//
//        Assert.isInstanceOf(InternalUserIdentity.class, registration,
//                "registration must be an instance of internal user identity");
//        InternalUserIdentity reg = (InternalUserIdentity) registration;
//
//        InternalUserPassword password = null;
//        if (credentials != null) {
//            Assert.isInstanceOf(InternalUserPassword.class, credentials,
//                    "credentials must be an instance of internal user password");
//            password = (InternalUserPassword) credentials;
//        }
//
//        // check email for confirmation when required
//        if (config.isConfirmationRequired()) {
//            if (reg.getEmailAddress() == null) {
//                throw new MissingDataException("email");
//            }
//
//            String email = Jsoup.clean(reg.getEmailAddress(), Safelist.none());
//            if (!StringUtils.hasText(email)) {
//                throw new MissingDataException("email");
//            }
//        }
//
//        // check password when provided
//        if (password != null) {
//            passwordService.validatePassword(password.getPassword());
//        }
//
//        // registration is create but user-initiated
//        InternalUserIdentity identity = createIdentity(userId, registration);
//        InternalUserAccount account = identity.getAccount();
//        String username = account.getUsername();
//
//        if (config.isConfirmationRequired() && !account.isConfirmed()) {
//            account = accountService.verifyAccount(username);
//        }
//
//        // with optional credentials
//        if (password != null) {
//            passwordService.setPassword(username, password.getPassword(), false);
//        }
//
//        return identity;
//    }
//
//    private InternalUserIdentity createIdentity(
//            String userId, UserIdentity registration)
//            throws NoSuchUserException, RegistrationException {
//
//        // create is always enabled
//        if (registration == null) {
//            throw new RegistrationException();
//        }
//
//        Assert.isInstanceOf(InternalUserIdentity.class, registration,
//                "registration must be an instance of internal user identity");
//        InternalUserIdentity reg = (InternalUserIdentity) registration;
//
//        // check for account details
//        InternalUserAccount account = reg.getAccount();
//        if (account == null) {
//            throw new MissingDataException("account");
//        }
//
//        // validate base param, nothing to do when missing
//        String username = reg.getUsername();
//        if (StringUtils.hasText(username)) {
//            username = Jsoup.clean(username, Safelist.none());
//        }
//        if (!StringUtils.hasText(username)) {
//            throw new MissingDataException("username");
//        }
//        String emailAddress = reg.getEmailAddress();
//
//        // no additional attributes supported
//
//        // we expect subject to be valid, or null if we need to create
//        UserEntity user = null;
//        if (!StringUtils.hasText(userId)) {
//            String realm = getRealm();
//
//            userId = userEntityService.createUser(realm).getUuid();
//            user = userEntityService.addUser(userId, realm, username, emailAddress);
//            userId = user.getUuid();
//        } else {
//            // check if exists
//            userEntityService.getUser(userId);
//        }
//
//        // make sure userId is correct
//        account.setUserId(userId);
//
//        try {
//            // create internal account
//            account = accountService.createAccount(account);
//
//            // store and update attributes
//            // we shouldn't have additional attributes for internal
//
//            // use builder to properly map attributes
//            InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(),
//                    account);
//
//            // no attribute sets
//            // this identity has no credentials
//            return identity;
//        } catch (RegistrationException | IllegalArgumentException e) {
//            // cleanup subject if we created it
//            if (user != null) {
//                userEntityService.deleteUser(userId);
//            }
//
//            throw e;
//        }
//    }

    @Override
    public void deleteIdentity(String userId, String username) throws NoSuchUserException {
        // remove all credentials
        passwordService.deleteCredentials(username);

        // call super to remove account
        super.deleteIdentity(userId, username);
    }

    @Override
    public String getAuthenticationUrl() {
        // display url for internal form
        return getFormUrl();
    }

    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return InternalPasswordIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    public String getFormUrl() {
        return InternalPasswordIdentityAuthority.AUTHORITY_URL + "form/" + getProvider();
    }

    public String getResetUrl() {
        return getCredentialsService().getResetUrl();
    }

    public String getLoginForm() {
        return "password_form";
    }

    @Override
    public InternalLoginProvider getLoginProvider() {
        InternalLoginProvider ilp = new InternalLoginProvider(getProvider(), getRealm(), getName());
        ilp.setDescription(getDescription());

        // login url is always form display
        ilp.setLoginUrl(getFormUrl());
//        ilp.setRegistrationUrl(getRegistrationUrl());
        ilp.setResetUrl(getResetUrl());

        // form action is always login action
        ilp.setFormUrl(getLoginUrl());

        String template = config.displayAsButton() ? "button" : getLoginForm();
        ilp.setTemplate(template);

        // set position
        ilp.setPosition(getConfig().getPosition());

        return ilp;
    }

}
