package it.smartcommunitylab.aac.webauthn.provider;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProvider;
import it.smartcommunitylab.aac.internal.provider.InternalSubjectResolver;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;

public class WebAuthnIdentityProvider extends
        AbstractIdentityProvider<InternalUserIdentity, InternalUserAccount, WebAuthnUserAuthenticatedPrincipal, WebAuthnIdentityProviderConfigMap, WebAuthnIdentityProviderConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final WebAuthnIdentityProviderConfig config;

    // services
    private final WebAuthnIdentityCredentialsService credentialsService;

    // providers
    private final WebAuthnIdentityAuthenticationProvider authenticationProvider;
    private final InternalAccountProvider accountProvider;
    private final InternalAttributeProvider<WebAuthnUserAuthenticatedPrincipal> attributeProvider;
    private final InternalSubjectResolver subjectResolver;

    public WebAuthnIdentityProvider(
            String providerId,
            UserAccountService<InternalUserAccount> userAccountService,
            WebAuthnUserCredentialsService userCredentialsService,
            WebAuthnIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, userAccountService, config, realm);
        Assert.notNull(userCredentialsService, "credentials service is mandatory");

        String repositoryId = config.getRepositoryId();
        logger.debug("create webauthn provider with id {} repository {}", String.valueOf(providerId), repositoryId);
        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new InternalAttributeProvider<>(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        this.accountProvider = new InternalAccountProvider(SystemKeys.AUTHORITY_WEBAUTHN, providerId,
                userAccountService, repositoryId, realm);

        // build providers
        this.credentialsService = new WebAuthnIdentityCredentialsService(providerId, userAccountService,
                userCredentialsService, config, realm);
        this.authenticationProvider = new WebAuthnIdentityAuthenticationProvider(providerId, accountProvider,
                credentialsService, config, realm);

        // always expose a valid resolver to satisfy authenticationManager at post login
        // TODO refactor to avoid fetching via resolver at this stage
        this.subjectResolver = new InternalSubjectResolver(providerId, userAccountService, repositoryId, false, realm);

    }

    @Override
    public boolean isAuthoritative() {
        // webauthn handles only login
        return false;
    }

    @Override
    public WebAuthnIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public WebAuthnIdentityAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public WebAuthnIdentityCredentialsService getCredentialsService() {
        return credentialsService;
    }

    @Override
    public InternalAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public InternalAttributeProvider<WebAuthnUserAuthenticatedPrincipal> getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public InternalSubjectResolver getSubjectResolver() {
        return subjectResolver;
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
            try {
                List<WebAuthnUserCredential> credentials = credentialsService
                        .findCredentialsByUsername(account.getUsername());
                credentials.forEach(c -> c.eraseCredentials());
                identity.setCredentials(credentials);
            } catch (NoSuchUserException e) {
                // this should not happen
                logger.error("no user for account {}", String.valueOf(account.getUsername()));
            }
        }

        return identity;
    }

    // TODO remove and set accountProvider read-only (and let create fail in super)
    @Override
    @Transactional(readOnly = false)
    public InternalUserIdentity convertIdentity(UserAuthenticatedPrincipal authPrincipal, String userId)
            throws NoSuchUserException {
        Assert.isInstanceOf(WebAuthnUserAuthenticatedPrincipal.class, authPrincipal, "Wrong principal class");
        logger.debug("convert principal to identity for user {}", String.valueOf(userId));
        if (logger.isTraceEnabled()) {
            logger.trace("principal {}", String.valueOf(authPrincipal));
        }

        WebAuthnUserAuthenticatedPrincipal principal = (WebAuthnUserAuthenticatedPrincipal) authPrincipal;

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

    @Override
    public void deleteIdentity(String userId, String username) throws NoSuchUserException {
        // remove all credentials
        credentialsService.deleteCredentialsByUsername(username);

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

        // form action is always login action
        ilp.setFormUrl(getLoginUrl());

        String template = config.displayAsButton() ? "button" : getLoginForm();
        ilp.setTemplate(template);

        // set position
        ilp.setPosition(getConfig().getPosition());

        return ilp;
    }

}
