package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCIdentityProvider extends AbstractProvider implements IdentityProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final OIDCIdentityProviderConfig config;

    // providers
    private final OIDCAccountProvider accountProvider;
    private final OIDCAttributeProvider attributeProvider;
    private final OIDCAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

    // attributes
    private final OpenIdAttributesMapper openidMapper;
    private ScriptExecutionService executionService;

    public OIDCIdentityProvider(
            String providerId,
            OIDCUserAccountRepository accountRepository, AttributeStore attributeStore,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountRepository, attributeStore, config, realm);
    }

    public OIDCIdentityProvider(
            String authority, String providerId,
            OIDCUserAccountRepository accountRepository, AttributeStore attributeStore,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(attributeStore, "attribute store is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new OIDCAccountProvider(authority, providerId, accountRepository, config, realm);
        this.attributeProvider = new OIDCAttributeProvider(authority, providerId, accountRepository, attributeStore,
                config,
                realm);
        this.authenticationProvider = new OIDCAuthenticationProvider(authority, providerId, accountRepository, config,
                realm);
        this.subjectResolver = new OIDCSubjectResolver(authority, providerId, accountRepository, config, realm);

        this.openidMapper = new OpenIdAttributesMapper();

    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
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
    @Transactional(readOnly = false)
    public OIDCUserIdentity convertIdentity(UserAuthenticatedPrincipal userPrincipal, String userId)
            throws NoSuchUserException {
        // we expect an instance of our model
        Assert.isInstanceOf(OIDCUserAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of internal authenticated principal");
        OIDCUserAuthenticatedPrincipal principal = (OIDCUserAuthenticatedPrincipal) userPrincipal;

        // we use upstream subject for accounts
        String subject = principal.getSubject();
        String provider = getProvider();

        // attributes from provider
        String username = principal.getUsername();
        Map<String, Serializable> attributes = principal.getAttributes();

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // get all attributes from principal except jwt attrs
        // TODO handle all attributes not only strings.
        Map<String, Serializable> principalAttributes = attributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(JWT_ATTRIBUTES, e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        // let hook process custom mapping
        if (executionService != null && config.getHookFunctions() != null
                && StringUtils.hasText(config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION))) {

            try {
                // execute script
                String functionCode = config.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION);
                Map<String, Serializable> customAttributes = executionService.executeFunction(
                        ATTRIBUTE_MAPPING_FUNCTION,
                        functionCode, principalAttributes);

                // update map
                if (customAttributes != null) {
                    // replace map
                    principalAttributes = customAttributes.entrySet().stream()
                            .filter(e -> !ArrayUtils.contains(JWT_ATTRIBUTES, e.getKey()))
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                }
            } catch (SystemException | InvalidDefinitionException ex) {
                logger.debug("error mapping principal attributes via script: " + ex.getMessage());
            }
        }

        // rebuild user principal with updated attributes
        OAuth2User oauth2User = principal.getOAuth2User();
        principal = new OIDCUserAuthenticatedPrincipal(getAuthority(), getProvider(),
                getRealm(),
                userId, subject);
        principal.setUsername(username);
        principal.setPrincipal(oauth2User);
        principal.setAttributes(principalAttributes);

        // map attributes to openid set and flatten to string
        AttributeSet oidcAttributeSet = openidMapper.mapAttributes(principalAttributes);
        Map<String, String> oidcAttributes = oidcAttributeSet.getAttributes()
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getKey(),
                        a -> a.exportValue()));

        String email = oidcAttributes.get(OpenIdAttributesSet.EMAIL);
        username = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME))
                ? oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME)
                : principal.getUsername();

        principal.setUsername(username);
        principal.setEmail(email);

        // TODO handle not persisted configuration
        //
        // look in repo or create
        OIDCUserAccount account = accountProvider.getAccount(subject);

        if (account == null) {
            // create
            account = new OIDCUserAccount();
            account.setSubject(subject);
            account.setUsername(username);
            account.setEmail(email);
            account = accountProvider.registerAccount(userId, account);
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

        // update additional attributes
        String issuer = attributes.containsKey(IdTokenClaimNames.ISS) ? attributes.get(IdTokenClaimNames.ISS).toString()
                : null;
        if (!StringUtils.hasText(issuer)) {
            issuer = provider;
        }

        String name = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.NAME))
                ? oidcAttributes.get(OpenIdAttributesSet.NAME)
                : username;

        String familyName = oidcAttributes.get(OpenIdAttributesSet.FAMILY_NAME);
        String givenName = oidcAttributes.get(OpenIdAttributesSet.GIVEN_NAME);

        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
                ? config.getConfigMap().getTrustEmailAddress()
                : false;
        boolean emailVerified = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                ? Boolean.parseBoolean(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }
        principal.setEmailVerified(emailVerified);

        String lang = oidcAttributes.get(OpenIdAttributesSet.LOCALE);
        // TODO evaluate how to handle external pictureURI
        String picture = oidcAttributes.get(OpenIdAttributesSet.PICTURE);

        // we override these every time
        account.setIssuer(issuer);
        account.setUsername(username);
        account.setName(name);
        account.setFamilyName(familyName);
        account.setGivenName(givenName);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setLang(lang);
        account.setPicture(picture);

        account = accountProvider.updateAccount(subject, account);

        // convert attribute sets via provider, will update store
        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal, userId);

        // build identity
        OIDCUserIdentity identity = new OIDCUserIdentity(getAuthority(), getProvider(), getRealm(), account, principal);
        identity.setAttributes(identityAttributes);

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserIdentity getIdentity(String subject) throws NoSuchUserException {
        return getIdentity(subject, true);
    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserIdentity getIdentity(String subject, boolean fetchAttributes)
            throws NoSuchUserException {
        // lookup a matching account
        OIDCUserAccount account = accountProvider.getAccount(subject);

        // build identity
        OIDCUserIdentity identity = new OIDCUserIdentity(getAuthority(), getProvider(), getRealm(), account);
        if (fetchAttributes) {
            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(subject);
            identity.setAttributes(identityAttributes);
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<OIDCUserIdentity> listIdentities(String userId) {
        return listIdentities(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<OIDCUserIdentity> listIdentities(String userId, boolean fetchAttributes) {
        // TODO handle not persisted configuration
        // lookup for matching accounts
        List<OIDCUserAccount> accounts = accountProvider.listAccounts(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<OIDCUserIdentity> identities = new ArrayList<>();

        for (OIDCUserAccount account : accounts) {
            // build identity
            OIDCUserIdentity identity = new OIDCUserIdentity(getAuthority(), getProvider(), getRealm(), account);
            if (fetchAttributes) {
                // convert attribute sets
                Collection<UserAttributes> identityAttributes = attributeProvider
                        .getAccountAttributes(account.getSubject());
                identity.setAttributes(identityAttributes);
            }

            identities.add(identity);
        }

        return identities;
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String subject) throws NoSuchUserException {
        // cleanup attributes
        attributeProvider.deleteAccountAttributes(subject);

        // delete account
        accountProvider.deleteAccount(subject);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String userId) {
        Collection<OIDCUserAccount> accounts = accountProvider.listAccounts(userId);
        for (OIDCUserAccount account : accounts) {
            try {
                deleteIdentity(account.getSubject());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        // TODO move url build to helper class
        return "/auth/" + getAuthority() + "/authorize/" + getProvider();
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
        // not configurable for now
        return SystemKeys.DISPLAY_MODE_BUTTON;
    }

    @Override
    public Map<String, String> getActionUrls() {
        return Collections.singletonMap(SystemKeys.ACTION_LOGIN, getAuthenticationUrl());
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

}
