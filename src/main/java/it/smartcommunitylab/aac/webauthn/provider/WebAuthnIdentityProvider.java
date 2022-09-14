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
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;

public class WebAuthnIdentityProvider extends
        AbstractIdentityProvider<InternalUserIdentity, InternalUserAccount, WebAuthnUserAuthenticatedPrincipal, WebAuthnIdentityProviderConfigMap, WebAuthnIdentityProviderConfig>
        implements IdentityCredentialsProvider<InternalUserAccount, WebAuthnCredential> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final WebAuthnIdentityProviderConfig config;

    // services
    protected final UserAccountService<InternalUserAccount> userAccountService;
    private final WebAuthnCredentialsService credentialsService;

    // providers
    protected final InternalAccountProvider accountProvider;
    protected final InternalAttributeProvider<WebAuthnUserAuthenticatedPrincipal> attributeProvider;
    protected final SubjectResolver<InternalUserAccount> subjectResolver;
    private final WebAuthnAuthenticationProvider authenticationProvider;

    public WebAuthnIdentityProvider(
            String providerId,
            UserEntityService userEntityService, UserAccountService<InternalUserAccount> userAccountService,
            SubjectService subjectService,
            WebAuthnCredentialsRepository credentialsRepository,
            WebAuthnIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId,
                userEntityService, userAccountService, subjectService,
                config, realm);
        Assert.notNull(credentialsRepository, "credentials repository is mandatory");

        logger.debug("create webauthn provider with id {}", String.valueOf(providerId));
        this.config = config;

        InternalIdentityProviderConfig internalConfig = config.toInternalProviderConfig();

        // internal data repositories
        this.userAccountService = userAccountService;

        // build providers
        this.credentialsService = new WebAuthnCredentialsService(providerId, userAccountService, credentialsRepository,
                config,
                realm);
        this.authenticationProvider = new WebAuthnAuthenticationProvider(providerId, userAccountService,
                credentialsService, config, realm);

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new InternalAttributeProvider<>(SystemKeys.AUTHORITY_WEBAUTHN, providerId,
                internalConfig, realm);
        this.accountProvider = new InternalAccountProvider(SystemKeys.AUTHORITY_WEBAUTHN, providerId,
                userAccountService, internalConfig, realm);

        // always expose a valid resolver to satisfy authenticationManager at post login
        // TODO refactor to avoid fetching via resolver at this stage
        this.subjectResolver = new InternalSubjectResolver(providerId, userAccountService, internalConfig, realm);

    }

    @Override
    public boolean isAuthoritative() {
        // webauthn handles only login
        return false;
    }

    @Override
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
    public InternalAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public InternalAttributeProvider<WebAuthnUserAuthenticatedPrincipal> getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public SubjectResolver<InternalUserAccount> getSubjectResolver() {
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
            List<WebAuthnCredential> credentials = credentialsService
                    .findActiveCredentialsByUsername(account.getUsername());
            credentials.forEach(c -> c.eraseCredentials());
            identity.setCredentials(credentials);
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

        // form action is always login action
        ilp.setFormUrl(getLoginUrl());

        String template = config.displayAsButton() ? "button" : getLoginForm();
        ilp.setTemplate(template);

        // set position
        ilp.setPosition(getConfig().getPosition());

        return ilp;
    }

}
