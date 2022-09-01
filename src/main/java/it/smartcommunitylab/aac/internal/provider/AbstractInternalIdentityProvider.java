package it.smartcommunitylab.aac.internal.provider;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.ScopeableProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public abstract class AbstractInternalIdentityProvider<P extends InternalUserAuthenticatedPrincipal, C extends UserCredentials>
        extends AbstractIdentityProvider<InternalUserIdentity, InternalUserAccount, P>
        implements ScopeableProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final InternalIdentityProviderConfig config;

    // services
    protected final UserAccountService<InternalUserAccount> userAccountService;

    // providers
    protected final InternalAccountProvider accountProvider;
    protected final InternalAttributeProvider<P> attributeProvider;
    protected final SubjectResolver<InternalUserAccount> subjectResolver;

    public AbstractInternalIdentityProvider(
            String providerId,
            UserEntityService userEntityService, UserAccountService<InternalUserAccount> userAccountService,
            SubjectService subjectService,
            InternalIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_INTERNAL, providerId, userEntityService, userAccountService, subjectService,
                config, realm);
    }

    public AbstractInternalIdentityProvider(
            String authority, String providerId,
            UserEntityService userEntityService, UserAccountService<InternalUserAccount> userAccountService,
            SubjectService subjectService,
            InternalIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, userEntityService, userAccountService, subjectService, config, realm);

        logger.debug("create internal provider for authority {} with id {}", String.valueOf(authority),
                String.valueOf(providerId));
        this.config = config;

        // internal data repositories
        this.userAccountService = userAccountService;

        // build resource providers, we use our providerId to ensure consistency
        this.attributeProvider = new InternalAttributeProvider<P>(providerId, config, realm);

        this.accountProvider = new InternalAccountProvider(authority, providerId, userAccountService,
                config, realm);

        // always expose a valid resolver to satisfy authenticationManager at post login
        // TODO refactor to avoid fetching via resolver at this stage
        this.subjectResolver = new InternalSubjectResolver(providerId, userAccountService, config, realm);
    }

//    public void setMailService(MailService mailService) {
//        // assign to services
//        this.accountService.setMailService(mailService);
//    }
//
//    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
//        // assign to services
//        this.accountService.setUriBuilder(uriBuilder);
//    }

    @Override
    public String getScope() {
        return config.getScope();
    }

    @Override
    protected String getRepositoryId() {
        return config.getRepositoryId();
    }

    @Override
    public InternalAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public InternalAttributeProvider<P> getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public SubjectResolver<InternalUserAccount> getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    protected InternalUserIdentity buildIdentity(InternalUserAccount account, P principal,
            Collection<UserAttributes> attributes) {
        // build identity
        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account,
                principal);
        identity.setAttributes(attributes);

        return identity;
    }

    @Override
    @Transactional(readOnly = false)
    public InternalUserIdentity convertIdentity(UserAuthenticatedPrincipal authPrincipal, String userId)
            throws NoSuchUserException {

        logger.debug("convert principal to identity for user {}", String.valueOf(userId));
        if (logger.isTraceEnabled()) {
            logger.trace("principal {}", String.valueOf(authPrincipal));
        }

        // cast principal and handle errors
        P principal = null;
        try {
            @SuppressWarnings("unchecked")
            P p = (P) authPrincipal;
            principal = p;
        } catch (ClassCastException e) {
            logger.error("Wrong principal class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported principal");
        }

        // sanity check for same authority
        if (!getAuthority().equals(principal.getAuthority())) {
            throw new IllegalArgumentException("authority mismatch");
        }

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

//    @Override
//    @Transactional(readOnly = true)
//    public InternalUserIdentity findIdentityByUuid(String uuid) {
//        // lookup a matching account
//        InternalUserAccount account = accountService.findAccountByUuid(uuid);
//        if (account == null) {
//            return null;
//        }
//        // build identity without attributes
//        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account);
//        return identity;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public InternalUserIdentity findIdentity(String username) {
//        // lookup a matching account
//        InternalUserAccount account = accountService.findAccount(username);
//        if (account == null) {
//            return null;
//        }
//        // build identity without attributes
//        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account);
//        return identity;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public InternalUserIdentity getIdentity(String username) throws NoSuchUserException {
//        return getIdentity(username, true);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public InternalUserIdentity getIdentity(String username, boolean fetchAttributes)
//            throws NoSuchUserException {
//
//        // lookup a matching account
//        InternalUserAccount account = accountService.getAccount(username);
////
////        // check subject
////        if (!account.getUserId().equals(userId)) {
////            throw new IllegalArgumentException("user mismatch");
////        }
//
//        // fetch attributes
//        // we shouldn't have additional attributes for internal
//
//        // use builder to properly map attributes
//        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account);
//        if (fetchAttributes) {
//            // convert attribute sets
//            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
//            identity.setAttributes(identityAttributes);
//        }
//
////        // do note returned identity has credentials populated
////        // we erase here
////        identity.eraseCredentials();
//
//        return identity;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Collection<InternalUserIdentity> listIdentities(String userId) {
//        return listIdentities(userId, true);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Collection<InternalUserIdentity> listIdentities(String userId, boolean fetchAttributes) {
//        // lookup for matching accounts
//        List<InternalUserAccount> accounts = accountService.listAccounts(userId);
//        if (accounts.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        List<InternalUserIdentity> identities = new ArrayList<>();
//        for (InternalUserAccount account : accounts) {
//            // fetch attributes
//            // TODO, we shouldn't have additional attributes for internal
//
//            // use builder to properly map attributes
//            InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(),
//                    account);
//            if (fetchAttributes) {
//                // convert attribute sets
//                Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
//                identity.setAttributes(identityAttributes);
//            }
//
////            // do note returned identity has credentials populated
////            // we erase here
////            identity.eraseCredentials();
//
//            identities.add(identity);
//        }
//
//        return identities;
//    }

//    @Override
//    @Transactional(readOnly = false)
//    public void deleteIdentity(String userId, String username) throws NoSuchUserException {
//        logger.debug("delete identity with username {}", String.valueOf(username));
//        // delete account
//        // TODO evaluate with shared accounts who deletes the registration
//        String repositoryId = getRepositoryId();
//        InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
//        if (account != null) {
//            // check userId matches
//            if (!account.getUserId().equals(userId)) {
//                throw new IllegalArgumentException("user mismatch");
//            }
//
//            String uuid = account.getUuid();
//            if (uuid != null) {
//                // remove subject if exists
//                subjectService.deleteSubject(uuid);
//            }
//
//            // remove account
//            userAccountService.deleteAccount(repositoryId, username);
//        }
//
//        // no attributes, but call delete anyways
//        getAttributeProvider().deleteAccountAttributes(username);
//    }

//    @Override
//    @Transactional(readOnly = false)
//    public void deleteIdentities(String userId) {
////        if (!config.getConfigMap().isEnableDelete()) {
////            throw new IllegalArgumentException("delete is disabled for this provider");
////        }
//
//        List<InternalUserAccount> accounts = accountService.listAccounts(userId);
//        for (InternalUserAccount account : accounts) {
//            try {
//                deleteIdentity(account.getUsername());
//            } catch (NoSuchUserException e) {
//            }
//        }
//    }
//
//    public void shutdown() {
//        // cleanup ourselves
//        // nothing to do
//    }
//
//    @Override
//    @Transactional(readOnly = false)
//    public InternalUserIdentity createIdentity(
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
//        try {
//            // create internal account
//            account = accountService.createAccount(userId, account);
//
//            // store and update attributes
//            // we shouldn't have additional attributes for internal
//
//            // use builder to properly map attributes
//            InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(),
//                    account);
//
//            // convert attribute sets
//            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
//            identity.setAttributes(identityAttributes);
//
//            // this identity has credentials
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
//
//    @Override
//    @Transactional(readOnly = false)
//    public InternalUserIdentity updateIdentity(
//            String userId,
//            String username, UserIdentity registration)
//            throws NoSuchUserException, RegistrationException {
//        if (!config.isEnableUpdate()) {
//            throw new IllegalArgumentException("update is disabled for this provider");
//        }
//
//        if (registration == null) {
//            throw new RegistrationException();
//        }
//
//        Assert.isInstanceOf(InternalUserIdentity.class, registration,
//                "registration must be an instance of internal user identity");
//        InternalUserIdentity reg = (InternalUserIdentity) registration;
//        if (reg.getAccount() == null) {
//            throw new MissingDataException("account");
//        }
//
//        // get the internal account entity
//        InternalUserAccount account = accountService.getAccount(username);
//
//        // check if userId matches account
//        if (!account.getUserId().equals(userId)) {
//            throw new RegistrationException("userid-mismatch");
//        }
//
//        // update account
//        account = accountService.updateAccount(username, reg.getAccount());
//
//        // store and update attributes
//        // we shouldn't have additional attributes for internal
//
//        // use builder to properly map attributes
//        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account);
//
//        // convert attribute sets
//        Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
//        identity.setAttributes(identityAttributes);
//
//        return identity;
//    }
//
//    @Override
//    public InternalUserIdentity linkIdentity(String userId, String username) throws NoSuchUserException {
//        // get the internal account entity
//        InternalUserAccount account = accountService.getAccount(username);
//
//        // re-link to new userId
//        account = accountService.linkAccount(username, userId);
//
//        // use builder, skip attributes
//        InternalUserIdentity identity = new InternalUserIdentity(getAuthority(), getProvider(), getRealm(), account);
//        return identity;
//    }

    public String getRegistrationUrl() {
        // TODO filter
        // TODO build a realm-bound url, need updates on filters
        return "/auth/" + getAuthority() + "/register/" + getProvider();
    }

}
