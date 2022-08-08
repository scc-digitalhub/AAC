package it.smartcommunitylab.aac.core.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.IdentityAttributeProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.model.Subject;

@Transactional
public abstract class AbstractIdentityProvider<I extends UserIdentity, U extends UserAccount, P extends UserAuthenticatedPrincipal>
        extends AbstractProvider
        implements IdentityProvider<I>, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    protected final UserEntityService userEntityService;
    protected final UserAccountService<U> userAccountService;
    protected final SubjectService subjectService;

    // provider configuration
    private final AbstractIdentityProviderConfig config;

    public AbstractIdentityProvider(
            String authority, String providerId,
            UserEntityService userEntityService, UserAccountService<U> userAccountService,
            SubjectService subjectService,
            AbstractIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        Assert.isTrue(authority.equals(config.getAuthority()),
                "configuration does not match this provider");
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        logger.debug("create {} idp for realm {} with id {}", String.valueOf(authority), String.valueOf(realm),
                String.valueOf(providerId));

        // internal data repositories
        this.userEntityService = userEntityService;
        this.userAccountService = userAccountService;
        this.subjectService = subjectService;

        // config
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(getAuthenticationProvider(), "authentication provider is mandatory");
        Assert.notNull(getAccountProvider(), "account provider is mandatory");
        Assert.notNull(getAttributeProvider(), "attribute provider is mandatory");
        Assert.notNull(getSubjectResolver(), "subject provider is mandatory");
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public boolean isAuthoritative() {
        // by default every provider is authoritative
        return true;
    }

    /*
     * Provider-specific
     */
    @Override
    public abstract ExtendedAuthenticationProvider<P, U> getAuthenticationProvider();

    @Override
    public abstract AccountProvider<U> getAccountProvider();

    @Override
    public abstract IdentityAttributeProvider<P, U> getAttributeProvider();

    @Override
    public abstract SubjectResolver<U> getSubjectResolver();

    protected abstract I buildIdentity(U account, P principal, Collection<UserAttributes> attributes);

    protected I buildIdentity(U account, Collection<UserAttributes> attributes) {
        return buildIdentity(account, null, attributes);
    }

    protected String getRepositoryId() {
        // by default isolate every provider in repo via its own id
        // subclasses may override to share accounts in the same service between idps
        return getProvider();
    }

    /*
     * Idp
     */

    public I convertIdentity(UserAuthenticatedPrincipal authPrincipal, String userId)
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

        // extract local id from principal
        // we expect principalId to be == accountId == identityId
        String id = principal.getPrincipalId();
        String repositoryId = getRepositoryId();

        if (id == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // TODO evaluate creation of userEntity when empty
        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // base attributes from provider
        String username = principal.getUsername();
        String emailAddress = principal.getEmailAddress();

        logger.debug("principal for {} is {} email {}", String.valueOf(userId), String.valueOf(username),
                String.valueOf(emailAddress));

        // convert to account
        U reg = getAccountProvider().convertAccount(principal, userId);

        if (logger.isTraceEnabled()) {
            logger.trace("converted account: {}", String.valueOf(reg));
        }

        // check matching with principal attributes
        if (username != null && !username.equals(reg.getUsername())) {
            logger.error("username mismatch between principal and account");
            throw new IllegalArgumentException();
        }

        if (emailAddress != null && !emailAddress.equals(reg.getEmailAddress())) {
            logger.error("emailAddress mismatch between principal and account");
            throw new IllegalArgumentException();
        }

        // look in service for existing accounts
        U account = userAccountService.findAccountById(repositoryId, id);
        if (account == null) {
            if (reg instanceof AbstractAccount) {
                // create subject
                logger.debug("create new subject for id {}", String.valueOf(id));
                String uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
                Subject s = subjectService.addSubject(uuid, getRealm(), SystemKeys.RESOURCE_ACCOUNT, username);
                ((AbstractAccount) reg).setUuid(s.getSubjectId());
            }

            // create account
            // TODO add config flag to disable creation
            logger.debug("create as new account with id {}", String.valueOf(id));
            account = userAccountService.addAccount(repositoryId, id, reg);
        } else {
            // check if userId matches
            if (!userId.equals(account.getUserId())) {
//              // force link
//              // TODO re-evaluate
//              account.setSubject(subjectId);
//              account = accountRepository.save(account);
                throw new IllegalArgumentException("user mismatch");
            }

            // update
            logger.debug("update existing account with id {}", String.valueOf(id));
            account = userAccountService.updateAccount(repositoryId, id, reg);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("persisted account: {}", String.valueOf(account));
        }

        // uuid is available for persisted accounts
        String uuid = account.getUuid();
        // set uuid on principal when possible
        if (principal instanceof AbstractAuthenticatedPrincipal) {
            ((AbstractAuthenticatedPrincipal) principal).setUuid(uuid);
        }

        // convert attribute sets via provider, will update store
        logger.debug("convert principal and account to attributes via provider for {}", String.valueOf(id));
        Collection<UserAttributes> attributes = getAttributeProvider().convertPrincipalAttributes(principal,
                account);
        if (logger.isTraceEnabled()) {
            logger.trace("identity attributes: {}", String.valueOf(attributes));
        }

        // build identity
        logger.debug("build identity for user {} from account {}", String.valueOf(userId), String.valueOf(id));
        I identity = buildIdentity(account, principal, attributes);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

//    @Override
    @Transactional(readOnly = true)
    public I findIdentityByUuid(String userId, String uuid) {
        logger.debug("find identity for uuid {}", String.valueOf(uuid));

        // lookup a matching account
        U account = getAccountProvider().findAccountByUuid(uuid);
        if (account == null) {
            return null;
        }

        // check userId matches
        if (!account.getUserId().equals(userId)) {
            return null;
        }

        // build identity without attributes or principal
        I identity = buildIdentity(account, null);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public I findIdentity(String userId, String accountId) {
        logger.debug("find identity for id {}", String.valueOf(accountId));

        // lookup a matching account
        U account = getAccountProvider().findAccount(accountId);
        if (account == null) {
            return null;
        }

        // check userId matches
        if (!account.getUserId().equals(userId)) {
            return null;
        }

        // build identity without attributes or principal
        I identity = buildIdentity(account, null);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public I getIdentity(String userId, String accountId) throws NoSuchUserException {
        return getIdentity(userId, accountId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public I getIdentity(String userId, String accountId, boolean fetchAttributes)
            throws NoSuchUserException {
        logger.debug("get identity for id {} user {} with attributes {}", String.valueOf(accountId),
                String.valueOf(userId), String.valueOf(fetchAttributes));

        // lookup a matching account
        U account = getAccountProvider().getAccount(accountId);

        // check userId matches
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user mismatch");
        }

        Collection<UserAttributes> attributes = null;
        if (fetchAttributes) {
            // convert attribute sets and fetch from repo
            attributes = getAttributeProvider().getAccountAttributes(account);
        }

        // use builder to properly map attributes
        I identity = buildIdentity(account, attributes);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<I> listIdentities(String userId) {
        return listIdentities(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<I> listIdentities(String userId, boolean fetchAttributes) {
        logger.debug("list identities for user {} attributes {}", String.valueOf(userId),
                String.valueOf(fetchAttributes));

        // lookup for matching accounts
        Collection<U> accounts = getAccountProvider().listAccounts(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<I> identities = new ArrayList<>();
        for (U account : accounts) {
            Collection<UserAttributes> attributes = null;
            if (fetchAttributes) {
                // convert attribute sets and fetch from repo
                attributes = getAttributeProvider().getAccountAttributes(account);
            }

            I identity = buildIdentity(account, attributes);
            if (logger.isTraceEnabled()) {
                logger.trace("identity: {}", String.valueOf(identity));
            }

            identities.add(identity);
        }

        return identities;
    }

    @Override
    @Transactional(readOnly = false)
    public I linkIdentity(String userId, String accountId) throws NoSuchUserException {
        logger.debug("link identity with id {} to user {}", String.valueOf(accountId), String.valueOf(userId));

        // get the internal account entity
        U account = getAccountProvider().getAccount(accountId);

        if (isAuthoritative()) {
            // re-link to new userId
            account = getAccountProvider().linkAccount(accountId, userId);
        }

        // use builder, skip attributes
        I identity = buildIdentity(account, null);
        if (logger.isTraceEnabled()) {
            logger.trace("identity: {}", String.valueOf(identity));
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String userId, String accountId) throws NoSuchUserException {
        logger.debug("delete identity with id {}", String.valueOf(accountId));

        // delete account
        // authoritative deletes the registration with shared accounts
        String repositoryId = getRepositoryId();
        U account = userAccountService.findAccountById(repositoryId, accountId);
        if (account != null && isAuthoritative()) {
            // check userId matches
            if (!account.getUserId().equals(userId)) {
                throw new IllegalArgumentException("user mismatch");
            }

            String uuid = account.getUuid();
            if (uuid != null) {
                // remove subject if exists
                subjectService.deleteSubject(uuid);
            }

            // remove account
            userAccountService.deleteAccount(repositoryId, accountId);
        }

        // cleanup attributes
        getAttributeProvider().deleteAccountAttributes(accountId);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String userId) {
        logger.debug("delete identity for user {}", String.valueOf(userId));

        Collection<U> accounts = getAccountProvider().listAccounts(userId);
        for (U account : accounts) {
            try {
                deleteIdentity(userId, account.getAccountId());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

}
