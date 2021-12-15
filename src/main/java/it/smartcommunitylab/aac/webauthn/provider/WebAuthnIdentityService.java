package it.smartcommunitylab.aac.webauthn.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserIdentity;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnIdentityService extends AbstractProvider implements IdentityService {
    private final UserEntityService userEntityService;

    // provider configuration
    private final WebAuthnIdentityProviderConfig config;

    // providers
    private final WebAuthnAccountService accountService;
    private final WebAuthnAttributeProvider attributeProvider;
    private final WebAuthnAuthenticationProvider authenticationProvider;
    private final WebAuthnSubjectResolver subjectResolver;
    private final WebAuthnCredentialsService credentialService;

    public WebAuthnIdentityService(
            String providerId,
            WebAuthnUserAccountService userAccountService, UserEntityService userEntityService,
            WebAuthnIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        // webauthn data repositories
        // TODO: replace with service to support external repo
        // this.accountRepository = accountRepository;
        this.userEntityService = userEntityService;
        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new WebAuthnAttributeProvider(providerId, userAccountService, config, realm);
        this.accountService = new WebAuthnAccountService(providerId, userAccountService, config, realm);
        this.credentialService = new WebAuthnCredentialsService(providerId, userAccountService, config, realm);
        this.authenticationProvider = new WebAuthnAuthenticationProvider(providerId, userAccountService, accountService,
                credentialService, config, realm);
        this.subjectResolver = new WebAuthnSubjectResolver(providerId, userAccountService, config, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public ExtendedAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public WebAuthnAccountService getAccountProvider() {
        return accountService;
    }

    @Override
    public WebAuthnAttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public WebAuthnSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    public boolean canRegister() {
        return config.getConfigMap().isEnableRegistration();
    }

    @Override
    public boolean canUpdate() {
        return config.getConfigMap().isEnableUpdate();

    }

    @Override
    public WebAuthnAccountService getAccountService() {
        return accountService;
    }

    @Override
    public WebAuthnCredentialsService getCredentialsService() {
        return credentialService;
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
    public ConfigurableProperties getConfiguration() {
        return config;
    }

    @Override
    public String getDisplayMode() {
        return config.getDisplayMode() != null ? config.getDisplayMode() : SystemKeys.DISPLAY_MODE_FORM;
    }

    public String getResetUrl() {
        return getCredentialsService().getResetUrl();
    }

    @Override
    public Map<String, String> getActionUrls() {
        Map<String, String> map = new HashMap<>();
        map.put(SystemKeys.ACTION_LOGIN, getAuthenticationUrl());
        map.put(SystemKeys.ACTION_REGISTER, getRegistrationUrl());
        map.put(SystemKeys.ACTION_RESET, getResetUrl());

        return map;
    }

    @Override
    @Transactional(readOnly = false)
    public UserIdentity convertIdentity(UserAuthenticatedPrincipal userPrincipal, String subjectId)
            throws NoSuchUserException {
        // extract account and attributes in raw format from authenticated principal
        WebAuthnUserAuthenticatedPrincipal principal = (WebAuthnUserAuthenticatedPrincipal) userPrincipal;
        String userId = principal.getUserId();
        // String username = principal.getName();
        //
        // // userId should be username, check
        // if (!parseResourceId(userId).equals(username)) {
        // throw new NoSuchUserException();
        // }

        if (subjectId == null) {
            // this better exists
            throw new NoSuchUserException();

        }

        // get the webauthn account entity
        WebAuthnUserAccount account = accountService.getAccount(userId);

        if (account == null) {
            // error, user should already exists for authentication
            throw new NoSuchUserException();
        }

        // subjectId is always present, is derived from the same account table
        String curSubjectId = account.getSubject();

        if (!curSubjectId.equals(subjectId)) {
            // // force link
            // // TODO re-evaluate
            // account.setSubject(subjectId);
            // account = accountRepository.save(account);
            throw new IllegalArgumentException("subject mismatch");

        }

        // store and update attributes
        // TODO, we shouldn't have additional attributes for webauthn

        // use builder to properly map attributes
        WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account, principal);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.convertAttributes(principal, subjectId);
        identity.setAttributes(identityAttributes);

        // do note returned identity has credentials populated
        // consumers will need to eraseCredentials
        // we erase here
        identity.eraseCredentials();
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public UserIdentity getIdentity(String subject, String userId) throws NoSuchUserException {
        return getIdentity(subject, userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public UserIdentity getIdentity(String subject, String userId, boolean fetchAttributes) throws NoSuchUserException {
        // check if we are the providers
        if (!getProvider().equals(parseProviderId(userId))) {
            throw new IllegalArgumentException("invalid provider key in userId");
        }

        // lookup a matching account
        WebAuthnUserAccount account = accountService.getAccount(userId);

        // check subject
        if (!account.getSubject().equals(subject)) {
            throw new IllegalArgumentException("subject mismatch");
        }

        // fetch attributes
        // TODO, we shouldn't have additional attributes for WebAuthn

        // use builder to properly map attributes
        WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);
        if (fetchAttributes) {
            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAttributes(userId);
            identity.setAttributes(identityAttributes);
        }

        // do note returned identity has credentials populated
        // we erase here
        identity.eraseCredentials();

        return identity;

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends UserIdentity> listIdentities(String subject) {
        return listIdentities(subject, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends UserIdentity> listIdentities(String subject, boolean fetchAttributes) {
        // lookup for matching accounts
        List<WebAuthnUserAccount> accounts = accountService.listAccounts(subject);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<WebAuthnUserIdentity> identities = new ArrayList<>();

        for (WebAuthnUserAccount account : accounts) {

            // fetch attributes
            // TODO, we shouldn't have additional attributes for WebAuthn

            // use builder to properly map attributes
            WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);
            if (fetchAttributes) {
                // convert attribute sets
                Collection<UserAttributes> identityAttributes = attributeProvider
                        .getAttributes(account.getUserId());
                identity.setAttributes(identityAttributes);
            }

            // do note returned identity has credentials populated
            // we erase here
            identity.eraseCredentials();

            identities.add(identity);
        }

        return identities;
    }

    @Override
    public String getAuthenticationUrl() {
        if (SystemKeys.DISPLAY_MODE_FORM.equals(getDisplayMode())) {
            // action url for receiving post
            return getLoginUrl();
        } else {
            // display url for webauthn form
            return getFormUrl();
        }
    }

    @Override
    public String getRegistrationUrl() {
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "register/" + getProvider();
    }

    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    public String getFormUrl() {
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "form/" + getProvider();
    }

    @Override
    @Transactional(readOnly = false)
    public WebAuthnUserIdentity registerIdentity(String subject, UserAccount reg, Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.getConfigMap().isEnableRegistration()) {
            throw new IllegalArgumentException("registration is disabled for this provider");
        }

        if (reg == null) {
            throw new RegistrationException("empty or incomplete registration");
        }

        String realm = getRealm();

        // validate base param, nothing to do when missing
        String username = reg.getUsername();
        if (StringUtils.hasText(username)) {
            username = Jsoup.clean(username, Safelist.none());
        }
        if (!StringUtils.hasText(username)) {
            throw new RegistrationException("missing-username");
        }
        String emailAddress = reg.getEmailAddress();

        // we expect subject to be valid, or null if we need to create
        UserEntity user = null;
        if (!StringUtils.hasText(subject)) {
            subject = userEntityService.createUser(realm).getUuid();
            user = userEntityService.addUser(subject, realm, username, emailAddress);
            subject = user.getUuid();
        } else {
            // check if exists
            userEntityService.getUser(subject);
        }

        try {
            // create webauthn account
            WebAuthnUserAccount account = accountService.registerAccount(subject, reg);

            // set providerId since all webauthn accounts have the same
            account.setProvider(getProvider());

            // rewrite webauthn userId
            account.setUserId(exportInternalId(username));

            // store and update attributes
            // TODO, we shouldn't have additional attributes for webauthn

            // use builder to properly map attributes
            WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);

            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAttributes(account.getUserId());
            identity.setAttributes(identityAttributes);

            // this identity has credentials
            return identity;

        } catch (RegistrationException | IllegalArgumentException e) {
            // cleanup subject if we created it
            if (user != null) {
                userEntityService.deleteUser(subject);
            }

            throw e;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public WebAuthnUserIdentity updateIdentity(String subject, String userId, UserAccount reg,
            Collection<UserAttributes> attributes) throws NoSuchUserException, RegistrationException {
        if (!config.getConfigMap().isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        if (reg == null) {
            throw new RegistrationException("empty or incomplete registration");
        }

        // we expect subject to be valid
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("invalid subjectId");
        }

        userEntityService.getUser(subject);

        // get the webauthn account entity
        WebAuthnUserAccount account = accountService.getAccount(userId);

        // check subject
        if (!account.getSubject().equals(subject)) {
            throw new IllegalArgumentException("subject mismatch");
        }

        account = accountService.updateAccount(subject, reg);

        // store and update attributes
        // TODO, we shouldn't have additional attributes for webauthn

        // use builder to properly map attributes
        WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.getAttributes(userId);
        identity.setAttributes(identityAttributes);

        // this identity has credentials, erase
        identity.eraseCredentials();

        return identity;
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String subject, String userId) throws NoSuchUserException {
        if (!config.getConfigMap().isEnableDelete()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        // get the webauthn account entity
        WebAuthnUserAccount account = accountService.getAccount(userId);

        // check subject
        if (!account.getSubject().equals(subject)) {
            throw new IllegalArgumentException("subject mismatch");
        }

        accountService.deleteAccount(userId);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String subjectId) {
        if (!config.getConfigMap().isEnableDelete()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        List<WebAuthnUserAccount> accounts = accountService.listAccounts(subjectId);
        for (UserAccount account : accounts) {
            try {
                accountService.deleteAccount(account.getUserId());
            } catch (NoSuchUserException e) {
            }
        }
    }
}
