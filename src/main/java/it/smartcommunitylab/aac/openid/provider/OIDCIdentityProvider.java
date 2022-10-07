package it.smartcommunitylab.aac.openid.provider;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Transactional
public class OIDCIdentityProvider
        extends
        AbstractIdentityProvider<OIDCUserIdentity, OIDCUserAccount, OIDCUserAuthenticatedPrincipal, OIDCIdentityProviderConfigMap, OIDCIdentityProviderConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // providers
    private final OIDCAccountProvider accountProvider;
    private final OIDCAttributeProvider attributeProvider;
    private final OIDCAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

//    // attributes
//    protected final OpenIdAttributesMapper openidMapper;

    public OIDCIdentityProvider(
            String providerId,
            UserAccountService<OIDCUserAccount> userAccountService,
            AttributeStore attributeStore,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, userAccountService, attributeStore, config, realm);
    }

    public OIDCIdentityProvider(
            String authority, String providerId,
            UserAccountService<OIDCUserAccount> userAccountService,
            AttributeStore attributeStore,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, userAccountService, config, realm);
        Assert.notNull(attributeStore, "attribute store is mandatory");

        logger.debug("create oidc provider for authority {} with id {}", String.valueOf(authority),
                String.valueOf(providerId));

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new OIDCAccountProvider(authority, providerId, userAccountService, config, realm);
        this.attributeProvider = new OIDCAttributeProvider(authority, providerId, attributeStore, config, realm);
        this.authenticationProvider = new OIDCAuthenticationProvider(authority, providerId, userAccountService, config,
                realm);
        this.subjectResolver = new OIDCSubjectResolver(authority, providerId, userAccountService, config, realm);

//        this.openidMapper = new OpenIdAttributesMapper();

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
    public OIDCAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public OIDCAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public OIDCAccountProvider getAccountPrincipalConverter() {
        return accountProvider;
    }

    @Override
    public OIDCAttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public OIDCSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    protected OIDCUserIdentity buildIdentity(OIDCUserAccount account, OIDCUserAuthenticatedPrincipal principal,
            Collection<UserAttributes> attributes) {
        // build identity
        OIDCUserIdentity identity = new OIDCUserIdentity(getAuthority(), getProvider(), getRealm(), account,
                principal);
        identity.setAttributes(attributes);

        return identity;
    }

//    @Override
//    @Transactional(readOnly = false)
//    public OIDCUserIdentity convertIdentity(UserAuthenticatedPrincipal userPrincipal, String userId)
//            throws NoSuchUserException {
//        // we expect an instance of our model
//        Assert.isInstanceOf(OIDCUserAuthenticatedPrincipal.class, userPrincipal,
//                "principal must be an instance of internal authenticated principal");
//        OIDCUserAuthenticatedPrincipal principal = (OIDCUserAuthenticatedPrincipal) userPrincipal;
//
//        // we use upstream subject for accounts
//        String subject = principal.getSubject();
//        String provider = getProvider();
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
//        // map attributes to openid set and flatten to string
//        AttributeSet oidcAttributeSet = openidMapper.mapAttributes(attributes);
//        Map<String, String> oidcAttributes = oidcAttributeSet.getAttributes()
//                .stream()
//                .collect(Collectors.toMap(
//                        a -> a.getKey(),
//                        a -> a.exportValue()));
//
//        String email = oidcAttributes.get(OpenIdAttributesSet.EMAIL);
//        username = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME))
//                ? oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME)
//                : principal.getUsername();
//
//        principal.setUsername(username);
//        principal.setEmail(email);
//
//        // TODO handle not persisted configuration
//        //
//        // look in repo or create
//        OIDCUserAccount account = accountProvider.findAccount(subject);
//
//        if (account == null) {
//            // create
//            account = new OIDCUserAccount();
//            account.setSubject(subject);
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
//        String issuer = attributes.containsKey(IdTokenClaimNames.ISS) ? attributes.get(IdTokenClaimNames.ISS).toString()
//                : null;
//        if (!StringUtils.hasText(issuer)) {
//            issuer = provider;
//        }
//
//        String name = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.NAME))
//                ? oidcAttributes.get(OpenIdAttributesSet.NAME)
//                : username;
//
//        String familyName = oidcAttributes.get(OpenIdAttributesSet.FAMILY_NAME);
//        String givenName = oidcAttributes.get(OpenIdAttributesSet.GIVEN_NAME);
//
//        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
//                ? config.getConfigMap().getTrustEmailAddress()
//                : false;
//        boolean emailVerified = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
//                ? Boolean.parseBoolean(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
//                : defaultVerifiedStatus;
//
//        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
//            emailVerified = true;
//        }
//        principal.setEmailVerified(emailVerified);
//
//        String lang = oidcAttributes.get(OpenIdAttributesSet.LOCALE);
//        // TODO evaluate how to handle external pictureURI
//        String picture = oidcAttributes.get(OpenIdAttributesSet.PICTURE);
//
//        // we override these every time
//        account.setIssuer(issuer);
//        account.setUsername(username);
//        account.setName(name);
//        account.setFamilyName(familyName);
//        account.setGivenName(givenName);
//        account.setEmail(email);
//        account.setEmailVerified(emailVerified);
//        account.setLang(lang);
//        account.setPicture(picture);
//
//        account = accountProvider.updateAccount(subject, account);
//
//        // convert attribute sets via provider, will update store
//        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal,
//                account);
//
//        // build identity
//        OIDCUserIdentity identity = new OIDCUserIdentity(getAuthority(), getProvider(), getRealm(), account, principal);
//        identity.setAttributes(identityAttributes);
//
//        return identity;
//    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        // TODO move url build to helper class
        return "/auth/" + getAuthority() + "/authorize/" + getProvider();
    }

    @Override
    public OIDCLoginProvider getLoginProvider() {
        OIDCLoginProvider lp = new OIDCLoginProvider(getAuthority(), getProvider(), getRealm(), getName());
        lp.setTitleMap(getTitleMap());
        lp.setDescriptionMap(getDescriptionMap());

        lp.setLoginUrl(getAuthenticationUrl());
        lp.setPosition(getConfig().getPosition());

        return lp;
    }

}
