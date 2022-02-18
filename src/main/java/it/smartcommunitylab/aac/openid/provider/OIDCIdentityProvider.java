package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.openid.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCIdentityProvider extends AbstractProvider implements IdentityProvider {

    // services
    private final OIDCUserAccountRepository accountRepository;
    private final AttributeStore attributeStore;

    private final OIDCIdentityProviderConfig providerConfig;

    // internal providers
    private final OIDCAccountProvider accountProvider;
    private final OIDCAttributeProvider attributeProvider;
    private final OIDCAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

    // attributes
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

        // internal data repositories
        this.accountRepository = accountRepository;
        this.attributeStore = attributeStore;

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");
        this.providerConfig = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new OIDCAccountProvider(authority, providerId, accountRepository, config, realm);
        this.attributeProvider = new OIDCAttributeProvider(authority, providerId, accountRepository, attributeStore,
                config,
                realm);
        this.authenticationProvider = new OIDCAuthenticationProvider(authority, providerId, accountRepository, config,
                realm);
        this.subjectResolver = new OIDCSubjectResolver(authority, providerId, accountRepository, config, realm);

    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public ConfigurableProperties getConfiguration() {
        return providerConfig;
    }

    @Override
    public String getName() {
        return providerConfig.getName();
    }

    @Override
    public String getDescription() {
        return providerConfig.getDescription();
    }

    @Override
    public ExtendedAuthenticationProvider getAuthenticationProvider() {
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
    public OIDCUserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException {
        // we expect an instance of our model
        OIDCAuthenticatedPrincipal user = (OIDCAuthenticatedPrincipal) principal;
        // we use upstream id for accounts
        String userId = parseResourceId(user.getUserId());
        String username = user.getName();
        String realm = getRealm();
        String provider = getProvider();
        Map<String, String> attributes = user.getAttributes();

        if (subjectId == null) {
            // this better exists
            throw new NoSuchUserException();

        }

        // TODO handle not persisted configuration
        //
        // look in repo or create
        OIDCUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);

        if (account == null) {
            account = new OIDCUserAccount();
            account.setSubject(subjectId);
            account.setUserId(userId);
            account.setProvider(provider);
            account.setRealm(realm);
            account = accountRepository.saveAndFlush(account);
        } else {
            // force link
            // TODO re-evaluate
            account.setSubject(subjectId);
        }

        String issuer = attributes.get(IdTokenClaimNames.ISS);
        if (!StringUtils.hasText(issuer)) {
            issuer = provider;
        }
        account.setIssuer(issuer);

        // get all attributes from principal except jwt attrs
        // TODO handle all attributes not only strings.
        Map<String, Serializable> principalAttributes = attributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(JWT_ATTRIBUTES, e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        // let hook process custom mapping
        if (executionService != null && providerConfig.getHookFunctions() != null
                && StringUtils.hasText(providerConfig.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION))) {

            try {
                // execute script
                String functionCode = providerConfig.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION);
                Map<String, Serializable> customAttributes = executionService.executeFunction(
                        ATTRIBUTE_MAPPING_FUNCTION,
                        functionCode, principalAttributes);

                // update map
                if (customAttributes != null) {
                    // TODO handle non string
                    Stream<Entry<String, ? extends Serializable>> attrstream = Stream.concat(
                            attributes.entrySet().stream()
                                    .filter(e -> ArrayUtils.contains(JWT_ATTRIBUTES, e.getKey())),
                            customAttributes.entrySet().stream()
                                    .filter(e -> !ArrayUtils.contains(JWT_ATTRIBUTES, e.getKey())));

                    Map<String, String> eattributes = attrstream
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

                    // rebuild user principal with updated attributes
                    OAuth2User oauth2User = user.getOAuth2User();
                    user = new OIDCAuthenticatedPrincipal(provider, realm, user.getUserId());
                    user.setName(username);
                    user.setPrincipal(oauth2User);
                    user.setAttributes(eattributes);

                    // replace map
                    principalAttributes = customAttributes.entrySet().stream()
                            .filter(e -> !ArrayUtils.contains(JWT_ATTRIBUTES, e.getKey()))
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

                    // TODO handle non string
                    attributes = user.getAttributes();
                }
            } catch (SystemException | InvalidDefinitionException ex) {
//                logger.error(ex.getMessage());
            }

        }

        // update account attributes
        // fetch from principal attributes - exact match only
        String name = attributes.get(OpenIdAttributesSet.NAME);
        String familyName = attributes.get(OpenIdAttributesSet.FAMILY_NAME);
        String givenName = attributes.get(OpenIdAttributesSet.GIVEN_NAME);
        String email = attributes.get(OpenIdAttributesSet.EMAIL);

        boolean defaultVerifiedStatus = providerConfig.getConfigMap().getTrustEmailAddress() != null
                ? providerConfig.getConfigMap().getTrustEmailAddress()
                : false;
        boolean emailVerified = StringUtils.hasText(attributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                ? Boolean.parseBoolean(attributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(providerConfig.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }

        String lang = attributes.get(OpenIdAttributesSet.LOCALE);
        // TODO evaluate how to handle external pictureURI
        String picture = attributes.get(OpenIdAttributesSet.PICTURE);

        // we override these every time
        account.setUsername(username);
        account.setName(name);
        account.setFamilyName(familyName);
        account.setGivenName(givenName);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setLang(lang);
        account.setPicture(picture);

        account = accountRepository.saveAndFlush(account);

        // update additional attributes in store, remove stale
        Set<Entry<String, Serializable>> userAttributes = principalAttributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(ACCOUNT_ATTRIBUTES, e.getKey()))
                .collect(Collectors.toSet());

        Set<Entry<String, Serializable>> storeAttributes = new HashSet<>();
        for (Entry<String, Serializable> e : userAttributes) {
            Entry<String, Serializable> es = new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue());
            storeAttributes.add(es);
        }

        attributeStore.setAttributes(userId, storeAttributes);

        // build identity
        // detach account
        account = accountRepository.detach(account);

        // export userId
        account.setUserId(exportInternalId(userId));

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.convertAttributes(user, subjectId);

        // write custom model
        OIDCUserIdentity identity = new OIDCUserIdentity(getProvider(), getRealm(), user);
        identity.setAccount(account);
        identity.setAttributes(identityAttributes);

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserIdentity getIdentity(String subject, String userId) throws NoSuchUserException {
        return getIdentity(subject, userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserIdentity getIdentity(String subject, String userId, boolean fetchAttributes)
            throws NoSuchUserException {
        OIDCUserAccount account = accountProvider.getAccount(userId);

        if (!account.getSubject().equals(subject)) {
            throw new NoSuchUserException();
        }

        // write custom model
        OIDCUserIdentity identity = new OIDCUserIdentity(getProvider(), getRealm());
        identity.setAccount(account);
        identity.setEmailAddress(account.getEmail());

        if (fetchAttributes) {
            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAttributes(userId);
            identity.setAttributes(identityAttributes);
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<OIDCUserIdentity> listIdentities(String subject) {
        return listIdentities(subject, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<OIDCUserIdentity> listIdentities(String subject, boolean fetchAttributes) {
        // TODO handle not persisted configuration
        List<OIDCUserIdentity> identities = new ArrayList<>();

        Collection<OIDCUserAccount> accounts = accountProvider.listAccounts(subject);

        for (OIDCUserAccount account : accounts) {
            // write custom model
            OIDCUserIdentity identity = new OIDCUserIdentity(getProvider(), getRealm());
            identity.setAccount(account);
            identity.setEmailAddress(account.getEmail());

            if (fetchAttributes) {
                // convert attribute sets
                Collection<UserAttributes> identityAttributes = attributeProvider
                        .getAttributes(account.getUserId());
                identity.setAttributes(identityAttributes);
            }

            identities.add(identity);
        }

        return identities;

    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String subjectId, String userId) throws NoSuchUserException {

        // delete account
        accountProvider.deleteAccount(userId);

        // cleanup attributes
        // direct access since we inserted these
        String id = parseResourceId(userId);
        attributeStore.deleteAttributes(id);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String subjectId) {
        Collection<OIDCUserAccount> accounts = accountProvider.listAccounts(subjectId);
        for (OIDCUserAccount account : accounts) {
            try {
                deleteIdentity(subjectId, account.getUserId());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return OIDCIdentityAuthority.AUTHORITY_URL + "authorize/" + getProvider();
    }

//    @Override
//    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
//        // we don't have one
//        return null;
//    }

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
            OpenIdAttributesSet.EMAIL,
            OpenIdAttributesSet.EMAIL_VERIFIED,
            OpenIdAttributesSet.PICTURE,
            OpenIdAttributesSet.LOCALE
    };

}
