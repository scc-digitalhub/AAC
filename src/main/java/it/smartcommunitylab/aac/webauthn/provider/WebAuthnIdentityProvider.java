package it.smartcommunitylab.aac.webauthn.provider;

import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.IdentityCredentialsProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.AbstractInternalIdentityProvider;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;

public class WebAuthnIdentityProvider
        extends AbstractInternalIdentityProvider<WebAuthnUserAuthenticatedPrincipal, WebAuthnCredential>
        implements IdentityCredentialsProvider<InternalUserAccount, WebAuthnCredential> {

    private final WebAuthnCredentialsService credentialsService;
    private final WebAuthnAuthenticationProvider authenticationProvider;

    // provider configuration
    private final WebAuthnIdentityProviderConfig config;

    public WebAuthnIdentityProvider(
            String providerId,
            UserEntityService userEntityService, UserAccountService<InternalUserAccount> userAccountService,
            SubjectService subjectService,
            WebAuthnCredentialsRepository credentialsRepository,
            WebAuthnIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId,
                userEntityService, userAccountService, subjectService,
                config.toInternalProviderConfig(), realm);
        Assert.notNull(credentialsRepository, "credentials repository is mandatory");

        this.config = config;

        // build providers
        this.credentialsService = new WebAuthnCredentialsService(providerId, userAccountService, credentialsRepository,
                config,
                realm);
        this.authenticationProvider = new WebAuthnAuthenticationProvider(providerId, userAccountService,
                credentialsService, config, realm);
    }

    @Override
    public boolean isAuthoritative() {
        // webauthn handles only login
        return false;
    }

    public CredentialsType getCredentialsType() {
        return CredentialsType.WEBAUTHN;
    }

    @Override
    public WebAuthnIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public WebAuthnAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public WebAuthnCredentialsService getCredentialsService() {
        return credentialsService;
    }

    @Override
    protected InternalUserIdentity buildIdentity(InternalUserAccount account,
            WebAuthnUserAuthenticatedPrincipal principal, Collection<UserAttributes> attributes) {
        // build identity
        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account,
                principal);
        identity.setAttributes(attributes);

        // if attributes then load credentials
        if (attributes != null) {
            List<WebAuthnCredential> credentials = credentialsService
                    .findActiveCredentialsByUsername(account.getUsername());
            credentials.forEach(c -> c.eraseCredentials());
            identity.setCredentials(credentials);
        }

        return identity;
    }

//    @Override
//    @Transactional(readOnly = false)
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
//            passwordService.setCredentials(username, password);
//        }
//
//        return identity;
//    }
//
    @Override
    public void deleteIdentity(String userId, String username) throws NoSuchUserException {
        // remove all credentials
        credentialsService.deleteCredentials(username);

        // call super to remove account
        super.deleteIdentity(userId, username);
    }

    @Override
    public String getAuthenticationUrl() {
        // display url for internal form
        return getFormUrl();
    }

    public String getLoginForm() {
        return "webauthn_form";
    }

    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    public String getFormUrl() {
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "form/" + getProvider();
    }

    @Override
    public InternalLoginProvider getLoginProvider() {
        InternalLoginProvider ilp = new InternalLoginProvider(getProvider(), getRealm(), getName());
        ilp.setDescription(getDescription());

        // login url is always form display
        ilp.setLoginUrl(getFormUrl());
        ilp.setRegistrationUrl(getRegistrationUrl());

        // form action is always login action
        ilp.setFormUrl(getLoginUrl());

        String template = config.displayAsButton() ? "button" : getLoginForm();
        ilp.setTemplate(template);

        // set position
        ilp.setPosition(getConfig().getPosition());

        return ilp;
    }

}
