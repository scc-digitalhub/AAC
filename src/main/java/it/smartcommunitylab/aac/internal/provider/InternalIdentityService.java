package it.smartcommunitylab.aac.internal.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

public abstract class InternalIdentityService<C extends UserCredentials> extends AbstractProvider
        implements IdentityService<InternalUserIdentity, InternalUserAccount, C> {

    // services
    protected final UserEntityService userEntityService;

    // provider configuration
    protected final InternalIdentityProviderConfig config;

    // providers
    protected final InternalAccountService accountService;
    protected final InternalAttributeProvider attributeProvider;
    protected final InternalSubjectResolver subjectResolver;

    public InternalIdentityService(
            String providerId,
            InternalUserAccountService userAccountService,
            UserEntityService userEntityService, SubjectService subjectService,
            InternalIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        // internal data repositories
        this.userEntityService = userEntityService;
        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new InternalAttributeProvider(providerId, config, realm);
        this.accountService = new InternalAccountService(providerId, userAccountService, subjectService, config, realm);
        this.subjectResolver = new InternalSubjectResolver(providerId, userAccountService, config, realm);
    }

    public void setMailService(MailService mailService) {
        // assign to services
        this.accountService.setMailService(mailService);
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        // assign to services
        this.accountService.setUriBuilder(uriBuilder);
    }

    public CredentialsType getCredentialsType() {
        return config.getCredentialsType();
    }

    public abstract String getLoginForm();

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public InternalIdentityProviderConfig getConfig() {
        return config;
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
        // TODO, we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account, principal);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal,
                account);
        identity.setAttributes(identityAttributes);

//        // do note returned identity has credentials populated
//        // consumers will need to eraseCredentials
//        // we erase here
//        identity.eraseCredentials();
        return identity;

    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserIdentity findIdentityByUuid(String uuid) {
        // lookup a matching account
        InternalUserAccount account = accountService.findAccountByUuid(uuid);
        if (account == null) {
            return null;
        }
        // build identity without attributes
        InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account);
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserIdentity findIdentity(String username) {
        // lookup a matching account
        InternalUserAccount account = accountService.findAccount(username);
        if (account == null) {
            return null;
        }
        // build identity without attributes
        InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account);
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
            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
            identity.setAttributes(identityAttributes);
        }

//        // do note returned identity has credentials populated
//        // we erase here
//        identity.eraseCredentials();

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
                Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
                identity.setAttributes(identityAttributes);
            }

//            // do note returned identity has credentials populated
//            // we erase here
//            identity.eraseCredentials();

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
        // display url for internal form
        return getFormUrl();
    }

    public void shutdown() {
        // cleanup ourselves
        // nothing to do
    }

    @Override
    public InternalAccountService getAccountService() {
        return accountService;
    }

    @Override
    @Transactional(readOnly = false)
    public InternalUserIdentity registerIdentity(
            String userId, UserAccount registration,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("registration is disabled for this provider");
        }

        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserAccount.class, registration,
                "registration must be an instance of internal user account");

        InternalUserAccount reg = (InternalUserAccount) registration;

        String realm = getRealm();

        // validate base param, nothing to do when missing
        String username = reg.getUsername();
        if (StringUtils.hasText(username)) {
            username = Jsoup.clean(username, Safelist.none());
        }
        if (!StringUtils.hasText(username)) {
            throw new MissingDataException("username");
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
            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
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
            String userId,
            String username, UserAccount registration,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserAccount.class, registration,
                "registration must be an instance of internal user account");

        InternalUserAccount reg = (InternalUserAccount) registration;

        // get the internal account entity
        InternalUserAccount account = accountService.getAccount(username);

        // check if userId matches account
        if (!account.getUserId().equals(userId)) {
            throw new RegistrationException("userid-mismatch");
        }

        account = accountService.updateAccount(username, reg);

        // store and update attributes
        // we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account);

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
        identity.setAttributes(identityAttributes);

//        // this identity has credentials, erase
//        identity.eraseCredentials();

        return identity;
    }

    @Override
    public InternalUserIdentity linkIdentity(String userId, String username) throws NoSuchUserException {
        // get the internal account entity
        InternalUserAccount account = accountService.getAccount(username);

        // re-link to new userId
        account = accountService.linkAccount(username, userId);

        // use builder, skip attributes
        InternalUserIdentity identity = new InternalUserIdentity(getProvider(), getRealm(), account);
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

        String template = config.displayAsButton() ? "button" : getLoginForm();
        ilp.setTemplate(template);

        return ilp;
    }

}
