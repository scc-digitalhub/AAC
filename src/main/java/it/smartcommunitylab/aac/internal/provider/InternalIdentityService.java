package it.smartcommunitylab.aac.internal.provider;

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
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

public class InternalIdentityService extends AbstractProvider implements IdentityService {

    // services
    private final UserEntityService userEntityService;

    // provider configuration
    private final InternalIdentityProviderConfig config;

    // providers
    private final InternalAccountService accountService;
    private final InternalAttributeProvider attributeProvider;
    private final InternalAuthenticationProvider authenticationProvider;
    private final InternalSubjectResolver subjectResolver;
    private final InternalPasswordService passwordService;

    public InternalIdentityService(
            String providerId,
            InternalUserAccountService userAccountService, UserEntityService userEntityService,
            InternalIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        // internal data repositories
        this.userEntityService = userEntityService;
        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new InternalAttributeProvider(providerId, userAccountService, config, realm);
        this.accountService = new InternalAccountService(providerId, userAccountService, config, realm);
        this.passwordService = new InternalPasswordService(providerId, userAccountService, config, realm);
        this.authenticationProvider = new InternalAuthenticationProvider(providerId, userAccountService, accountService,
                passwordService, config, realm);
        this.subjectResolver = new InternalSubjectResolver(providerId, userAccountService, config, realm);

    }

    public void setMailService(MailService mailService) {
        // assign to services
        this.accountService.setMailService(mailService);
        this.passwordService.setMailService(mailService);
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        // also assign to services
        this.accountService.setUriBuilder(uriBuilder);
        this.passwordService.setUriBuilder(uriBuilder);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public InternalAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public InternalAccountService getAccountProvider() {
        return accountService;
    }

    @Override
    public InternalAttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public InternalSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    @Transactional(readOnly = false)
    public InternalUserIdentity convertIdentity(UserAuthenticatedPrincipal userPrincipal, String userId)
            throws NoSuchUserException {
        Assert.isInstanceOf(InternalUserAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of internal authenticated principal");

        // extract account and attributes in raw format from authenticated principal
        InternalUserAuthenticatedPrincipal principal = (InternalUserAuthenticatedPrincipal) userPrincipal;

        // username binds all identity pieces together
        String username = principal.getUsername();

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // get the internal account entity
        InternalUserAccount account = accountService.getAccount(username);

        if (account == null) {
            // error, user should already exists for authentication
            throw new NoSuchUserException();
        }

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
        // TODO, we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account, principal);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal, userId);
        identity.setAttributes(identityAttributes);

        // do note returned identity has credentials populated
        // consumers will need to eraseCredentials
        // we erase here
        identity.eraseCredentials();
        return identity;

    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserIdentity getIdentity(String username) throws NoSuchUserException {
        return getIdentity(username, true);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserIdentity getIdentity(String username, boolean fetchAttributes)
            throws NoSuchUserException {

        // lookup a matching account
        InternalUserAccount account = accountService.getAccount(username);
//
//        // check subject
//        if (!account.getUserId().equals(userId)) {
//            throw new IllegalArgumentException("user mismatch");
//        }

        // fetch attributes
        // we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account);
        if (fetchAttributes) {
            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(username);
            identity.setAttributes(identityAttributes);
        }

        // do note returned identity has credentials populated
        // we erase here
        identity.eraseCredentials();

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InternalUserIdentity> listIdentities(String userId) {
        return listIdentities(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InternalUserIdentity> listIdentities(String userId, boolean fetchAttributes) {
        // lookup for matching accounts
        List<InternalUserAccount> accounts = accountService.listAccounts(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<InternalUserIdentity> identities = new ArrayList<>();
        for (InternalUserAccount account : accounts) {
            // fetch attributes
            // TODO, we shouldn't have additional attributes for internal

            // use builder to properly map attributes
            InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account);
            if (fetchAttributes) {
                // convert attribute sets
                Collection<UserAttributes> identityAttributes = attributeProvider
                        .getAccountAttributes(account.getUsername());
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
//        if (!config.isEnableDelete()) {
//            throw new IllegalArgumentException("delete is disabled for this provider");
//        }

//        // get the internal account entity
//        InternalUserAccount account = accountService.getAccount(username);

//        // check subject
//        if (!account.getUserId().equals(userId)) {
//            throw new IllegalArgumentException("user mismatch");
//        }

        accountService.deleteAccount(username);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String userId) {
//        if (!config.getConfigMap().isEnableDelete()) {
//            throw new IllegalArgumentException("delete is disabled for this provider");
//        }

        List<InternalUserAccount> accounts = accountService.listAccounts(userId);
        for (InternalUserAccount account : accounts) {
            try {
                accountService.deleteAccount(account.getUsername());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public String getAuthenticationUrl() {
        if (SystemKeys.DISPLAY_MODE_FORM.equals(getDisplayMode())) {
            // action url for receiving post
            return getLoginUrl();
        } else {
            // display url for internal form
            return getFormUrl();
        }
    }

//    @Override
//    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
//        // we don't have one
//        // TODO add
//        return null;
//    }

    public void shutdown() {
        // cleanup ourselves
        // nothing to do
    }

    @Override
    public boolean canRegister() {
        return config.isEnableRegistration();
    }

    @Override
    public boolean canUpdate() {
        return config.isEnableUpdate();

    }

    @Override
    public InternalAccountService getAccountService() {
        return accountService;
    }

    @Override
    public InternalPasswordService getCredentialsService() {
        return passwordService;
    }

    @Override
    @Transactional(readOnly = false)
    public InternalUserIdentity registerIdentity(
            String userId, UserAccount reg,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
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
        if (!StringUtils.hasText(userId)) {
            userId = userEntityService.createUser(realm).getUuid();
            user = userEntityService.addUser(userId, realm, username, emailAddress);
            userId = user.getUuid();
        } else {
            // check if exists
            userEntityService.getUser(userId);
        }

        try {
            // create internal account
            InternalUserAccount account = accountService.registerAccount(userId, reg);

            // store and update attributes
            // we shouldn't have additional attributes for internal

            // use builder to properly map attributes
            InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account);

            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider
                    .getAccountAttributes(account.getUsername());
            identity.setAttributes(identityAttributes);

            // this identity has credentials
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
    public InternalUserIdentity updateIdentity(
            String username, UserAccount reg,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        if (reg == null) {
            throw new RegistrationException("empty or incomplete registration");
        }

//        // we expect subject to be valid
//        if (!StringUtils.hasText(userId)) {
//            throw new IllegalArgumentException("invalid subjectId");
//        }
//
//        UserEntity user = userEntityService.getUser(userId);

        // get the internal account entity
        InternalUserAccount account = accountService.getAccount(username);

//        // check subject
//        if (!account.getUserId().equals(userId)) {
//            throw new IllegalArgumentException("subject mismatch");
//        }

        account = accountService.updateAccount(username, reg);

        // store and update attributes
        // we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(username);
        identity.setAttributes(identityAttributes);

        // this identity has credentials, erase
        identity.eraseCredentials();

        return identity;
    }

    @Override
    public String getRegistrationUrl() {
        // TODO filter
        // TODO build a realm-bound url, need updates on filters
        return InternalIdentityAuthority.AUTHORITY_URL + "register/" + getProvider();
    }

    public String getResetUrl() {
        return getCredentialsService().getResetUrl();
    }

    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return InternalIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    public String getFormUrl() {
        return InternalIdentityAuthority.AUTHORITY_URL + "form/" + getProvider();
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
        return config.getDisplayMode() != null ? config.getDisplayMode() : SystemKeys.DISPLAY_MODE_FORM;
    }

    @Override
    public Map<String, String> getActionUrls() {
        Map<String, String> map = new HashMap<>();
        map.put(SystemKeys.ACTION_LOGIN, getAuthenticationUrl());
        map.put(SystemKeys.ACTION_REGISTER, getRegistrationUrl());
        map.put(SystemKeys.ACTION_RESET, getResetUrl());

        return map;
    }
}
