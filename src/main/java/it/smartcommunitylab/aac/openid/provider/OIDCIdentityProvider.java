package it.smartcommunitylab.aac.openid.provider;

import java.util.Collection;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.dto.LoginProvider;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.service.OIDCUserAccountService;

@Transactional
public class OIDCIdentityProvider
        extends AbstractIdentityProvider<OIDCUserIdentity, OIDCUserAccount, OIDCUserAuthenticatedPrincipal> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final OIDCIdentityProviderConfig config;

    // providers
    private final OIDCAccountProvider accountProvider;
    private final OIDCAttributeProvider attributeProvider;
    private final OIDCAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

//    // attributes
//    protected final OpenIdAttributesMapper openidMapper;

    public OIDCIdentityProvider(
            String providerId,
            UserEntityService userEntityService, OIDCUserAccountService userAccountService,
            SubjectService subjectService,
            AttributeStore attributeStore,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, userEntityService, userAccountService, subjectService,
                attributeStore, config, realm);
    }

    public OIDCIdentityProvider(
            String authority, String providerId,
            UserEntityService userEntityService, OIDCUserAccountService userAccountService,
            SubjectService subjectService,
            AttributeStore attributeStore,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, userEntityService, userAccountService, subjectService, config, realm);
        Assert.notNull(attributeStore, "attribute store is mandatory");

        logger.debug("create oidc provider for authority {} with id {}", String.valueOf(authority),
                String.valueOf(providerId));
        this.config = config;

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
    public OIDCIdentityProviderConfig getConfig() {
        return config;
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
    public LoginProvider getLoginProvider() {
        LoginProvider lp = new LoginProvider(getAuthority(), getProvider(), getRealm());
        lp.setName(getName());
        lp.setDescription(getDescription());

        lp.setLoginUrl(getAuthenticationUrl());
        lp.setTemplate("button");

        String icon = "it-key";
        if (ArrayUtils.contains(ICONS, getAuthority())) {
            icon = "logo-" + getAuthority();
        }
        if (ArrayUtils.contains(ICONS, lp.getKey())) {
            icon = "logo-" + lp.getKey();
        }
        String iconUrl = icon.startsWith("logo-") ? "svg/sprite.svg#" + icon : "italia/svg/sprite.svg#" + icon;
        lp.setIcon(icon);
        lp.setIconUrl(iconUrl);

        return lp;
    }

    public static String[] JWT_ATTRIBUTES = {
            IdTokenClaimNames.ACR,
            IdTokenClaimNames.AMR,
            IdTokenClaimNames.AT_HASH,
            IdTokenClaimNames.AUD,
            IdTokenClaimNames.AUTH_TIME,
            IdTokenClaimNames.AZP,
            IdTokenClaimNames.C_HASH,
            IdTokenClaimNames.EXP,
            IdTokenClaimNames.IAT,
            IdTokenClaimNames.ISS,
            IdTokenClaimNames.NONCE,
            IdTokenClaimNames.SUB
    };

    public static String[] ACCOUNT_ATTRIBUTES = {
            "username",
            OpenIdAttributesSet.NAME,
            OpenIdAttributesSet.FAMILY_NAME,
            OpenIdAttributesSet.GIVEN_NAME,
            OpenIdAttributesSet.PREFERRED_USERNAME,
            OpenIdAttributesSet.EMAIL,
            OpenIdAttributesSet.EMAIL_VERIFIED,
            OpenIdAttributesSet.PICTURE,
            OpenIdAttributesSet.LOCALE
    };

    public static String[] ICONS = {
            "google", "facebook", "github", "microsoft", "apple", "instagram"
    };

}
