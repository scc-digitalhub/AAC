package it.smartcommunitylab.aac.internal.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.CredentialsService;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

public class InternalIdentityProvider extends AbstractProvider implements IdentityService {

    // services
    // TODO replace with internalUserService to abstract repo + remove core user
    // service, we don't want access to core here
    private final InternalUserAccountRepository accountRepository;
    private final UserEntityService userEntityService;

    // provider configuration
    private final InternalIdentityProviderConfig config;

    // providers
    private final InternalAccountProvider accountProvider;
    private final InternalAttributeProvider attributeProvider;
    private final InternalAuthenticationProvider authenticationProvider;
    private final InternalSubjectResolver subjectResolver;
    private final InternalAccountService accountService;
    private final InternalPasswordService passwordService;

    public InternalIdentityProvider(
            String providerId,
            InternalUserAccountRepository accountRepository, UserEntityService userEntityService,
            ConfigurableProvider configurableProvider, InternalIdentityProviderConfigMap defaultConfigMap,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");

        // internal data repositories
        // TODO replace with service to support external repo
        this.accountRepository = accountRepository;
        this.userEntityService = userEntityService;

        // translate configuration
        if (configurableProvider != null) {
            Assert.isTrue(SystemKeys.AUTHORITY_INTERNAL.equals(configurableProvider.getAuthority()),
                    "configuration does not match this provider");
            Assert.isTrue(providerId.equals(configurableProvider.getProvider()),
                    "configuration does not match this provider");
            Assert.isTrue(realm.equals(configurableProvider.getRealm()), "configuration does not match this provider");

            // merge config with default
            InternalIdentityProviderConfig providerConfig = InternalIdentityProviderConfig.fromConfigurableProvider(
                    configurableProvider,
                    defaultConfigMap);
            config = providerConfig;
        } else {
            // keep default
            InternalIdentityProviderConfig providerConfig = new InternalIdentityProviderConfig(providerId, realm);
            providerConfig.setConfigMap(defaultConfigMap);
            config = providerConfig;

        }

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new InternalAccountProvider(providerId, accountRepository, realm);
        this.accountService = new InternalAccountService(providerId, accountRepository, realm,
                config.getConfigMap());
        this.passwordService = new InternalPasswordService(providerId, accountRepository, realm,
                config.getConfigMap());

        // TODO attributeService to feed attribute provider
        this.attributeProvider = new InternalAttributeProvider(providerId, accountRepository, null, realm);
        this.authenticationProvider = new InternalAuthenticationProvider(providerId, accountRepository, realm);
        this.subjectResolver = new InternalSubjectResolver(providerId, accountRepository, realm);

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
    public AccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public AttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public SubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    public UserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException {
        // extract account and attributes in raw format from authenticated principal
        String userId = principal.getUserId();
        String username = principal.getName();

        // userId should be username, check
        if (!parseResourceId(userId).equals(username)) {
            throw new NoSuchUserException();
        }

        if (subjectId == null) {
            // this better exists
            throw new NoSuchUserException();

        }

        // get the internal account entity
        InternalUserAccount account = accountRepository.findByRealmAndUsername(getRealm(), username);

        if (account == null) {
            // error, user should already exists for authentication
            throw new NoSuchUserException();
        }

        // subjectId is always present, is derived from the same account table
        String curSubjectId = account.getSubject();

        if (!curSubjectId.equals(subjectId)) {
            // force link
            // TODO re-evaluate
            account.setSubject(subjectId);
            account = accountRepository.save(account);
        }

        // detach account
        account = accountRepository.detach(account);

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        // store and update attributes
        // TODO, we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        // TODO consolidate *all* attribute sets logic in attributeProvider
        InternalUserIdentity identity = InternalUserIdentity.from(getProvider(), account, getRealm());

        // do note returned identity has credentials populated
        // consumers will need to eraseCredentials
        // TODO evaluate erase here
        return identity;

    }

    @Override
    public UserIdentity getIdentity(String userId) throws NoSuchUserException {

        String username = parseResourceId(userId);
        // check if we are the providers
        if (!getProvider().equals(parseProviderId(userId))) {
            throw new IllegalArgumentException("invalid provider key in userId");
        }

        // lookup a matching account
        InternalUserAccount account = accountRepository.findByRealmAndUsername(getRealm(), username);

        if (account == null) {
            // error, user should already exists for authentication
            throw new NoSuchUserException();
        }

        // detach account
        account = accountRepository.detach(account);

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        // store and update attributes
        // TODO, we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        // TODO consolidate *all* attribute sets logic in attributeProvider
        InternalUserIdentity identity = InternalUserIdentity.from(getProvider(), account, getRealm());

        // do note returned identity has credentials populated
        // we erase here
        identity.eraseCredentials();

        return identity;

    }

    @Override
    public UserIdentity getIdentity(String userId, boolean fetchAttributes) throws NoSuchUserException {
        // no attributes for now
        return getIdentity(userId);
    }

    @Override
    public Collection<UserIdentity> listIdentities(String subject) {
        // lookup for matching accounts
        List<InternalUserAccount> accounts = accountRepository.findBySubjectAndRealm(subject, getRealm());
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserIdentity> identities = new ArrayList<>();

        for (InternalUserAccount account : accounts) {
            String username = account.getUsername();

            // detach account
            account = accountRepository.detach(account);

            // set providerId since all internal accounts have the same
            account.setProvider(getProvider());

            // rewrite internal userId
            account.setUserId(exportInternalId(username));

            // store and update attributes
            // TODO, we shouldn't have additional attributes for internal

            // use builder to properly map attributes
            // TODO consolidate *all* attribute sets logic in attributeProvider
            InternalUserIdentity identity = InternalUserIdentity.from(getProvider(), account, getRealm());

            // do note returned identity has credentials populated
            // we erase here
            identity.eraseCredentials();

            identities.add(identity);
        }

        return identities;
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return InternalIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        // we don't have one
        // TODO add
        return null;
    }

    public void shutdown() {
        // cleanup ourselves
        // nothing to do
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
    public boolean canDelete() {
        return config.getConfigMap().isEnableDelete();
    }

    @Override
    public AccountService getAccountService() {
        return accountService;
    }

    @Override
    public CredentialsService getCredentialsService() {
        return passwordService;
    }

    @Override
    public UserIdentity registerIdentity(String subject, Collection<Entry<String, String>> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.getConfigMap().isEnableRegistration()) {
            throw new IllegalArgumentException("registration is disabled for this provider");
        }

        // validate only mandatory attributes
        Map<String, String> map = new HashMap<>();
        attributes.forEach(e -> map.put(e.getKey(), e.getValue()));

        accountService.validateAttributes(map);

        String username = map.get("username");
        String email = map.get("email");
        String name = map.get("name");
        String realm = getRealm();

        // remediate missing username, we need to build subject
        if (!StringUtils.hasText(username)) {
            if (StringUtils.hasText(email)) {
                int idx = email.indexOf('@');
                if (idx > 0) {
                    username = email.substring(0, idx);
                }
            } else if (StringUtils.hasText(name)) {
                username = StringUtils.trimAllWhitespace(name);
            }
        }

        // we expect subject to be valid, or null if we need to create
        if (!StringUtils.hasText(subject)) {

            subject = userEntityService.createUser(realm).getUuid();
            UserEntity user = userEntityService.addUser(subject, realm, username);
        }

        // create account
        InternalUserAccount account = accountService.registerAccount(subject, attributes);

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        // store and update attributes
        // TODO, we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        // TODO consolidate *all* attribute sets logic in attributeProvider
        InternalUserIdentity identity = InternalUserIdentity.from(getProvider(), account, getRealm());

        // this identity has credentials
        return identity;

    }

    @Override
    public UserIdentity updateIdentity(String subject, String userId, Collection<Entry<String, String>> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.getConfigMap().isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        // we expect subject to be valid
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("invalid subjectId");
        }

        String username = parseResourceId(userId);
        String realm = getRealm();
        UserEntity user = userEntityService.getUser(subject);

        // get the internal account entity
        InternalUserAccount account = accountRepository.findByRealmAndUsername(getRealm(), username);

        if (account == null) {
            // error, user should already exists for authentication
            throw new NoSuchUserException();
        }

        // subjectId is always present, is derived from the same account table
        String curSubjectId = account.getSubject();

        if (!curSubjectId.equals(subject)) {
            throw new IllegalArgumentException("subject mismatch");
        }

        account = accountService.updateAccount(subject, userId, attributes);

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        // store and update attributes
        // TODO, we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        // TODO consolidate *all* attribute sets logic in attributeProvider
        InternalUserIdentity identity = InternalUserIdentity.from(getProvider(), account, getRealm());

        // this identity has credentials, erase
        identity.eraseCredentials();

        return identity;
    }

    @Override
    public void deleteIdentity(String subjectId, String userId) throws NoSuchUserException {
        if (!config.getConfigMap().isEnableDelete()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        accountService.deleteAccount(subjectId, userId);
    }

    @Override
    public void deleteIdentities(String subjectId) {
        if (!config.getConfigMap().isEnableDelete()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        Collection<UserAccount> accounts = accountProvider.listAccounts(subjectId);
        for (UserAccount account : accounts) {
            try {
                accountService.deleteAccount(subjectId, account.getUserId());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public String getRegistrationUrl() {
        // TODO filter
        // TODO build a realm-bound url, need updates on filters
        return InternalIdentityAuthority.AUTHORITY_URL + "register/" + getProvider();
    }

    @Override
    public String getName() {
        return config.getName();
    }

}
