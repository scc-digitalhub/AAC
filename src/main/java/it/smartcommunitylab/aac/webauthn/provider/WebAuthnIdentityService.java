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
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.utils.MailService;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserIdentity;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnIdentityService extends AbstractProvider
        implements IdentityService<WebAuthnUserIdentity, WebAuthnUserAccount> {

    // services
    private final UserEntityService userEntityService;

    // provider configuration
    private final WebAuthnIdentityProviderConfig config;

    // providers
    private final WebAuthnAccountService accountService;
    private final WebAuthnAttributeProvider attributeProvider;
    private final WebAuthnAuthenticationProvider authenticationProvider;
    private final WebAuthnSubjectResolver subjectResolver;

    public WebAuthnIdentityService(
            String providerId,
            WebAuthnUserAccountService userAccountService,
            UserEntityService userEntityService, SubjectService subjectService,
            WebAuthnIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        // webauthn data repositories
        this.userEntityService = userEntityService;
        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new WebAuthnAttributeProvider(providerId, config, realm);
        this.accountService = new WebAuthnAccountService(providerId, userAccountService, subjectService, config, realm);
        this.authenticationProvider = new WebAuthnAuthenticationProvider(providerId, userAccountService, config, realm);
        this.subjectResolver = new WebAuthnSubjectResolver(providerId, userAccountService, config, realm);

    }

    public void setMailService(MailService mailService) {
        // assign to services
        this.accountService.setMailService(mailService);
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        // also assign to services
        this.accountService.setUriBuilder(uriBuilder);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
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
    public WebAuthnAccountService getAccountService() {
        return accountService;
    }

    @Override
    @Transactional(readOnly = false)
    public WebAuthnUserIdentity convertIdentity(UserAuthenticatedPrincipal userPrincipal, String userId)
            throws NoSuchUserException {
        Assert.isInstanceOf(WebAuthnUserAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of internal authenticated principal");

        // extract account and attributes in raw format from authenticated principal
        WebAuthnUserAuthenticatedPrincipal principal = (WebAuthnUserAuthenticatedPrincipal) userPrincipal;

        // username binds all identity pieces together
        String username = principal.getUsername();

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // get the internal account entity
        WebAuthnUserAccount account = accountService.getAccount(username);

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
        // TODO, we shouldn't have additional attributes for webauthn

        // use builder to properly map attributes
        WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account, principal);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal,
                account);
        identity.setAttributes(identityAttributes);

        // do note returned identity may have credentials populated
        // we erase here
        identity.eraseCredentials();
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserIdentity findIdentityByUuid(String uuid) {
        // lookup a matching account
        WebAuthnUserAccount account = accountService.findAccountByUuid(uuid);
        if (account == null) {
            return null;
        }
        // build identity without attributes
        WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserIdentity findIdentity(String username) {
        // lookup a matching account
        WebAuthnUserAccount account = accountService.findAccount(username);
        if (account == null) {
            return null;
        }
        // build identity without attributes
        WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserIdentity getIdentity(String username) throws NoSuchUserException {
        return getIdentity(username, true);
    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserIdentity getIdentity(String username, boolean fetchAttributes)
            throws NoSuchUserException {

        // lookup a matching account
        WebAuthnUserAccount account = accountService.getAccount(username);
//
//        // check subject
//        if (!account.getUserId().equals(userId)) {
//            throw new IllegalArgumentException("user mismatch");
//        }

        // fetch attributes
        // we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);
        if (fetchAttributes) {
            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
            identity.setAttributes(identityAttributes);
        }

        // do note returned identity has credentials populated
        // we erase here
        identity.eraseCredentials();

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<WebAuthnUserIdentity> listIdentities(String userId) {
        return listIdentities(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<WebAuthnUserIdentity> listIdentities(String userId, boolean fetchAttributes) {
        // lookup for matching accounts
        List<WebAuthnUserAccount> accounts = accountService.listAccounts(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<WebAuthnUserIdentity> identities = new ArrayList<>();
        for (WebAuthnUserAccount account : accounts) {
            // fetch attributes
            // TODO, we shouldn't have additional attributes for internal

            // use builder to properly map attributes
            WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);
            if (fetchAttributes) {
                // convert attribute sets
                Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
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
    @Transactional(readOnly = false)
    public void deleteIdentity(String username) throws NoSuchUserException {
        accountService.deleteAccount(username);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String userId) {
        List<WebAuthnUserAccount> accounts = accountService.listAccounts(userId);
        for (WebAuthnUserAccount account : accounts) {
            try {
                accountService.deleteAccount(account.getUsername());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    @Transactional(readOnly = false)
    public WebAuthnUserIdentity registerIdentity(String userId, WebAuthnUserAccount reg,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
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
        String userHandle = reg.getUserHandle();
        if (StringUtils.hasText(userHandle)) {
            userHandle = Jsoup.clean(userHandle, Safelist.none());
        }
        if (!StringUtils.hasText(userHandle)) {
            throw new RegistrationException("missing-user-handle");
        }
        String emailAddress = reg.getEmailAddress();

        // we expect user to be valid, or null if we need to create
        UserEntity user = null;
        if (!StringUtils.hasText(userId)) {
            userId = userEntityService.createUser(realm).getUuid();
            user = userEntityService.addUser(userId, realm, username, emailAddress);
            userId = user.getUuid();
        } else {
            // check if exists
            userEntityService.getUser(userId);
        }

        try {
            // create webauthn account
            WebAuthnUserAccount account = accountService.registerAccount(userId, reg);

            // use builder to properly map attributes
            WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);

            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
            identity.setAttributes(identityAttributes);

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
    @Transactional(readOnly = false)
    public WebAuthnUserIdentity updateIdentity(
            String username, WebAuthnUserAccount reg,
            Collection<UserAttributes> attributes) throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        if (reg == null) {
            throw new RegistrationException("empty or incomplete registration");
        }
        // get the webauthn account entity
        WebAuthnUserAccount account = accountService.getAccount(username);
        account = accountService.updateAccount(username, reg);

        // use builder to properly map attributes
        WebAuthnUserIdentity identity = new WebAuthnUserIdentity(getProvider(), getRealm(), account);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
        identity.setAttributes(identityAttributes);

        // this identity may have credentials, erase
        identity.eraseCredentials();

        return identity;
    }

    @Override
    public String getAuthenticationUrl() {
        return getLoginUrl();
    }

    @Override
    public String getRegistrationUrl() {
        // we use an address bound to provider, no reason to expose realm
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "register/" + getProvider();
    }

    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return WebAuthnIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
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
    public String getDisplayMode() {
        // we support only this mode
        return "webauthn";
    }

    @Override
    public Map<String, String> getActionUrls() {
        Map<String, String> map = new HashMap<>();
        map.put(SystemKeys.ACTION_LOGIN, getAuthenticationUrl());
        map.put(SystemKeys.ACTION_REGISTER, getRegistrationUrl());

        return map;
    }

    @Override
    public UserCredentialsService getCredentialsService() {
        return null;
    }

}