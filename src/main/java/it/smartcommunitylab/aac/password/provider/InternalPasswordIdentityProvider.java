package it.smartcommunitylab.aac.password.provider;

import java.util.Collection;
import java.util.Collections;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.IdentityCredentialsProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.AbstractInternalIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAccountService;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.password.InternalPasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.model.InternalPasswordUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.service.InternalPasswordService;
import it.smartcommunitylab.aac.utils.MailService;

public class InternalPasswordIdentityProvider
        extends AbstractInternalIdentityProvider<InternalPasswordUserAuthenticatedPrincipal, InternalUserPassword>
        implements IdentityCredentialsProvider<InternalUserAccount, InternalUserPassword> {

    private final InternalPasswordService passwordService;
    private final InternalPasswordAuthenticationProvider authenticationProvider;

    // TODO remove, registration should go via no credentials provider
    protected final InternalAccountService accountService;

    // provider configuration
    private final InternalPasswordIdentityProviderConfig config;

    public InternalPasswordIdentityProvider(
            String providerId,
            UserEntityService userEntityService,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            SubjectService subjectService,
            InternalUserPasswordRepository passwordRepository,
            InternalPasswordIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId,
                userEntityService, userAccountService, subjectService,
                config, realm);
        Assert.notNull(passwordRepository, "password repository is mandatory");

        this.config = config;

        // build providers

        this.accountService = new InternalAccountService(SystemKeys.AUTHORITY_PASSWORD, providerId,
                userAccountService, confirmKeyService, subjectService,
                config, realm);

        this.passwordService = new InternalPasswordService(providerId, userAccountService, passwordRepository, config,
                realm);
        this.authenticationProvider = new InternalPasswordAuthenticationProvider(providerId, userAccountService,
                accountService, passwordService, config, realm);
    }

    public void setMailService(MailService mailService) {
        this.accountService.setMailService(mailService);
        this.passwordService.setMailService(mailService);
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.accountService.setUriBuilder(uriBuilder);
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

    /*
     * User registration
     * 
     * TODO remove and handle via internalIdentityService
     */

    public InternalUserIdentity registerIdentity(
            String userId, UserIdentity registration, UserCredentials credentials)
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

        InternalUserPassword password = null;
        if (credentials != null) {
            Assert.isInstanceOf(InternalUserPassword.class, credentials,
                    "credentials must be an instance of internal user password");
            password = (InternalUserPassword) credentials;
        }

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

        // check password when provided
        if (password != null) {
            passwordService.validatePassword(password.getPassword());
        }

        // registration is create but user-initiated
        InternalUserIdentity identity = createIdentity(userId, registration);
        InternalUserAccount account = identity.getAccount();
        String username = account.getUsername();

        if (config.isConfirmationRequired() && !account.isConfirmed()) {
            account = accountService.verifyAccount(username);
        }

        // with optional credentials
        if (password != null) {
            passwordService.setPassword(username, password.getPassword(), false);
        }

        return identity;
    }

    private InternalUserIdentity createIdentity(
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
        ilp.setRegistrationUrl(getRegistrationUrl());
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
