package it.smartcommunitylab.aac.password.provider;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.AbstractInternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityService;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.service.InternalPasswordService;
import it.smartcommunitylab.aac.utils.MailService;

public class PasswordIdentityService extends InternalIdentityService<InternalUserPassword> {

    private final InternalPasswordService passwordService;
    private final PasswordAuthenticationProvider authenticationProvider;

    // provider configuration
    private final PasswordIdentityProviderConfig config;

    public PasswordIdentityService(
            String providerId,
            InternalUserAccountService userAccountService,
            UserEntityService userEntityService, SubjectService subjectService,
            InternalUserPasswordRepository passwordRepository,
            PasswordIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId,
                userAccountService,
                userEntityService, subjectService,
                config, realm);
        Assert.notNull(passwordRepository, "password repository is mandatory");

        this.config = config;

        // build providers
        this.passwordService = new InternalPasswordService(providerId, userAccountService, passwordRepository, config,
                realm);
        this.authenticationProvider = new PasswordAuthenticationProvider(providerId, userAccountService, accountService,
                passwordService, config, realm);
    }

    @Override
    public void setMailService(MailService mailService) {
        super.setMailService(mailService);
        this.passwordService.setMailService(mailService);
    }

    @Override
    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        super.setUriBuilder(uriBuilder);
        this.passwordService.setUriBuilder(uriBuilder);
    }

    @Override
    public InternalIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public PasswordAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public InternalPasswordService getCredentialsService() {
        return passwordService;
    }

    @Override
    @Transactional(readOnly = false)
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
            passwordService.setCredentials(username, password);
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String username) throws NoSuchUserException {
        // remove all credentials
        passwordService.deleteCredentials(username);

        // call super to remove account
        super.deleteIdentity(username);
    }

    @Override
    public String getAuthenticationUrl() {
        // display url for internal form
        return getFormUrl();
    }

    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return AbstractInternalIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    public String getFormUrl() {
        return AbstractInternalIdentityAuthority.AUTHORITY_URL + "form/" + getProvider();
    }

    public String getResetUrl() {
        return getCredentialsService().getResetUrl();
    }

    @Override
    public InternalLoginProvider getLoginProvider() {
        InternalLoginProvider ilp = new InternalLoginProvider(getProvider(), getRealm());
        ilp.setName(getName());
        ilp.setDescription(getDescription());

        // login url is always form display
        ilp.setLoginUrl(getFormUrl());
        ilp.setRegistrationUrl(getRegistrationUrl());
        ilp.setResetUrl(getResetUrl());

        // form action is always login action
        ilp.setFormUrl(getLoginUrl());

        String template = config.displayAsButton() ? "button" : "password_form";
        ilp.setTemplate(template);

        return ilp;
    }

}
