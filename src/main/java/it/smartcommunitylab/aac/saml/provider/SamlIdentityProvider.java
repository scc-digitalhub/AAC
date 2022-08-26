package it.smartcommunitylab.aac.saml.provider;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

public class SamlIdentityProvider
        extends AbstractIdentityProvider<SamlUserIdentity, SamlUserAccount, SamlUserAuthenticatedPrincipal> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final SamlIdentityProviderConfig config;

    // internal providers
    private final SamlAccountProvider accountProvider;
    private final SamlAttributeProvider attributeProvider;
    private final SamlAuthenticationProvider authenticationProvider;
    private final SamlSubjectResolver subjectResolver;

    public SamlIdentityProvider(
            String providerId,
            UserEntityService userEntityService, UserAccountService<SamlUserAccount> userAccountService,
            SubjectService subjectService,
            AttributeStore attributeStore,
            SamlIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_SAML, providerId, userEntityService, userAccountService, subjectService,
                attributeStore, config, realm);
    }

    public SamlIdentityProvider(
            String authority, String providerId,
            UserEntityService userEntityService, UserAccountService<SamlUserAccount> userAccountService,
            SubjectService subjectService,
            AttributeStore attributeStore,
            SamlIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, userEntityService, userAccountService, subjectService, config,
                realm);
        Assert.notNull(attributeStore, "attribute store is mandatory");

        logger.debug("create saml provider with id {}", String.valueOf(providerId));
        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new SamlAccountProvider(authority, providerId, userAccountService, config, realm);
        this.attributeProvider = new SamlAttributeProvider(authority, providerId, attributeStore, config,
                realm);
        this.authenticationProvider = new SamlAuthenticationProvider(authority, providerId, userAccountService, config,
                realm);
        this.subjectResolver = new SamlSubjectResolver(authority, providerId, userAccountService, config, realm);

        // function hooks from config
        if (config.getHookFunctions() != null
                && StringUtils.hasText(config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION))) {
            this.authenticationProvider
                    .setCustomMappingFunction(config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION));
        }
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.authenticationProvider.setExecutionService(executionService);
    }

    @Override
    public SamlIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public SamlAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public SamlAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public SamlAttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public SamlSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    protected SamlUserIdentity buildIdentity(SamlUserAccount account, SamlUserAuthenticatedPrincipal principal,
            Collection<UserAttributes> attributes) {
        // build identity
        SamlUserIdentity identity = new SamlUserIdentity(getAuthority(), getProvider(), getRealm(), account, principal);
        identity.setAttributes(attributes);

        return identity;
    }

//    @Override
//    @Transactional(readOnly = false)
//    public SamlUserIdentity convertIdentity(UserAuthenticatedPrincipal userPrincipal, String userId)
//            throws NoSuchUserException {
//        // we expect an instance of our model
//        Assert.isInstanceOf(SamlUserAuthenticatedPrincipal.class, userPrincipal,
//                "principal must be an instance of internal authenticated principal");
//        SamlUserAuthenticatedPrincipal principal = (SamlUserAuthenticatedPrincipal) userPrincipal;
//
//        // we use upstream subject for accounts
//        // TODO handle transient ids, for example with session persistence
//        String subjectId = principal.getSubjectId();
//
//        if (userId == null) {
//            // this better exists
//            throw new NoSuchUserException();
//        }
//
//        // attributes from provider
//        String username = principal.getUsername();
//        Map<String, Serializable> attributes = principal.getAttributes();
//
//        // re-read attributes as-is, transform to strings
//        // TODO evaluate using a custom mapper to given profile
//        Map<String, String> samlAttributes = attributes.entrySet().stream()
//                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));
//
//        String email = samlAttributes.get("email");
//        username = StringUtils.hasText(samlAttributes.get("username"))
//                ? samlAttributes.get("username")
//                : principal.getUsername();
//
//        principal.setUsername(username);
//        principal.setEmail(email);
//
//        // TODO handle not persisted configuration
//        //
//        // look in repo or create
//        SamlUserAccount account = accountProvider.findAccount(subjectId);
//
//        if (account == null) {
//            // create
//            account = new SamlUserAccount();
//            account.setSubjectId(subjectId);
//            account.setUsername(username);
//            account.setEmail(email);
//            account = accountProvider.registerAccount(userId, account);
//        }
//
//        // uuid is available for persisted accounts
//        String uuid = account.getUuid();
//        principal.setUuid(uuid);
//
//        // userId is always present, is derived from the same account table
//        String curUserId = account.getUserId();
//
//        if (!curUserId.equals(userId)) {
////            // force link
////            // TODO re-evaluate
////            account.setSubject(subjectId);
////            account = accountRepository.save(account);
//            throw new IllegalArgumentException("user mismatch");
//        }
//
//        // update additional attributes
//        String issuer = samlAttributes.get("issuer");
//        if (!StringUtils.hasText(issuer)) {
//            issuer = config.getRelyingPartyRegistration().getAssertingPartyDetails().getEntityId();
//        }
//
//        String name = StringUtils.hasText(samlAttributes.get("name")) ? samlAttributes.get("name") : username;
//
//        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
//                ? config.getConfigMap().getTrustEmailAddress()
//                : false;
//        boolean emailVerified = StringUtils.hasText(samlAttributes.get("emailVerified"))
//                ? Boolean.parseBoolean(samlAttributes.get("emailVerified"))
//                : defaultVerifiedStatus;
//
//        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
//            emailVerified = true;
//        }
//        principal.setEmailVerified(emailVerified);
//
//        // we override these every time
//        account.setIssuer(issuer);
//        account.setUsername(username);
//        account.setName(name);
//        account.setEmail(email);
//        account.setEmailVerified(emailVerified);
//        account.setLang(null);
//
//        account = accountProvider.updateAccount(subjectId, account);
//
//        // convert attribute sets via provider, will update store
//        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal,
//                account);
//
//        // build identity
//        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account, principal);
//        identity.setAttributes(identityAttributes);
//
//        return identity;
//
//    }

//    @Override
//    @Transactional(readOnly = true)
//    public SamlUserIdentity findIdentityByUuid(String uuid) {
//        // lookup a matching account
//        SamlUserAccount account = accountProvider.findAccountByUuid(uuid);
//        if (account == null) {
//            return null;
//        }
//        // build identity without attributes
//        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account);
//        return identity;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public SamlUserIdentity findIdentity(String subjectId) {
//        // lookup a matching account
//        SamlUserAccount account = accountProvider.findAccount(subjectId);
//        if (account == null) {
//            return null;
//        }
//        // build identity without attributes
//        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account);
//        return identity;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public SamlUserIdentity getIdentity(String subjectId) throws NoSuchUserException {
//        return getIdentity(subjectId, true);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public SamlUserIdentity getIdentity(String subjectId, boolean fetchAttributes)
//            throws NoSuchUserException {
//        // lookup a matching account
//        SamlUserAccount account = accountProvider.getAccount(subjectId);
//
//        // build identity
//        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account);
//        if (fetchAttributes) {
//            // convert attribute sets
//            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
//            identity.setAttributes(identityAttributes);
//        }
//
//        return identity;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Collection<SamlUserIdentity> listIdentities(String userId) {
//        return listIdentities(userId, true);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Collection<SamlUserIdentity> listIdentities(String userId, boolean fetchAttributes) {
//        // TODO handle not persisted configuration
//        // lookup for matching accounts
//        List<SamlUserAccount> accounts = accountProvider.listAccounts(userId);
//        if (accounts.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        List<SamlUserIdentity> identities = new ArrayList<>();
//
//        for (SamlUserAccount account : accounts) {
//            // build identity
//            SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account);
//            if (fetchAttributes) {
//                // convert attribute sets
//                Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
//                identity.setAttributes(identityAttributes);
//            }
//
//            identities.add(identity);
//        }
//
//        return identities;
//    }
//
//    @Override
//    @Transactional(readOnly = false)
//    public SamlUserIdentity linkIdentity(String userId, String username) throws NoSuchUserException {
//        // get the internal account entity
//        SamlUserAccount account = accountProvider.getAccount(username);
//
//        // re-link to new userId
//        account = accountProvider.linkAccount(username, userId);
//
//        // use builder, skip attributes
//        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account);
//        return identity;
//    }
//
//    @Override
//    @Transactional(readOnly = false)
//    public void deleteIdentity(String subjectId) throws NoSuchUserException {
//        // cleanup attributes
//        attributeProvider.deleteAccountAttributes(subjectId);
//
//        // delete account
//        accountProvider.deleteAccount(subjectId);
//    }
//
//    @Override
//    @Transactional(readOnly = false)
//    public void deleteIdentities(String userId) {
//        Collection<SamlUserAccount> accounts = accountProvider.listAccounts(userId);
//        for (SamlUserAccount account : accounts) {
//            try {
//                deleteIdentity(account.getSubjectId());
//            } catch (NoSuchUserException e) {
//            }
//        }
//    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return "/auth/" + getAuthority() + "authenticate/" + getProvider();
    }

    @Override
    public SamlLoginProvider getLoginProvider() {
        SamlLoginProvider lp = new SamlLoginProvider(getAuthority(), getProvider(), getRealm(), getName());
        lp.setDescription(getDescription());
        lp.setLoginUrl(getAuthenticationUrl());
        lp.setPosition(getConfig().getPosition());

        return lp;
    }

    public static final String[] SAML_ATTRIBUTES = {
            "subject", "issuer", "issueInstant"
    };

    public static final String[] ACCOUNT_ATTRIBUTES = {
            "username",
            "name",
            "email",
            "locale"
    };
}
