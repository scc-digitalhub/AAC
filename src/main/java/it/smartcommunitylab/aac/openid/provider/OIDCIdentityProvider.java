package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.CredentialsService;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.openid.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;
import it.smartcommunitylab.aac.profiles.OpenIdProfileAttributesSet;

public class OIDCIdentityProvider extends AbstractProvider implements IdentityService {

    private final String providerName;

    // services
    private final OIDCUserAccountRepository accountRepository;
    private final AttributeStore attributeStore;
    private ScriptExecutionService executionService;

    private final OIDCIdentityProviderConfig providerConfig;

    // internal providers
    private final OIDCAccountProvider accountProvider;
    private final OIDCAttributeProvider attributeProvider;
    private final OIDCAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

    public OIDCIdentityProvider(
            String providerId, String providerName,
            OIDCUserAccountRepository accountRepository, AttributeStore attributeStore,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_OIDC, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.providerName = StringUtils.hasText(providerName) ? providerName : providerId;

        // internal data repositories
        this.accountRepository = accountRepository;
        this.attributeStore = attributeStore;

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");
        this.providerConfig = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new OIDCAccountProvider(providerId, accountRepository, realm);
        this.attributeProvider = new OIDCAttributeProvider(providerId, accountRepository, attributeStore, realm);
        this.authenticationProvider = new OIDCAuthenticationProvider(providerId, accountRepository, realm);
        this.subjectResolver = new OIDCSubjectResolver(providerId, accountRepository, realm);

    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
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
    @Transactional(readOnly = false)
    public OIDCUserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException {
        // we expect an instance of our model
        OIDCAuthenticatedPrincipal user = (OIDCAuthenticatedPrincipal) principal;
        // we use internal id for accounts
        String userId = parseResourceId(user.getUserId());
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
                // TODO handle non string
                attributes = customAttributes.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

            } catch (SystemException | InvalidDefinitionException ex) {
//                logger.error(ex.getMessage());
            }

        }

        // update base attributes
        // we build only openid profile, it supersedes basic
        OpenIdProfileAttributesSet openIdAttributeSet = new OpenIdProfileAttributesSet(SystemKeys.AUTHORITY_OIDC,
                provider, realm, exportInternalId(userId));

        // fetch from principal attributes
        String username = user.getName();
        String name = attributes.get(OpenIdProfileAttributesSet.NAME);
        String familyName = attributes.get(OpenIdProfileAttributesSet.FAMILY_NAME);
        String givenName = attributes.get(OpenIdProfileAttributesSet.GIVEN_NAME);
        String email = attributes.get(OpenIdProfileAttributesSet.EMAIL);
        boolean emailVerified = StringUtils.hasText(attributes.get(OpenIdProfileAttributesSet.EMAIL_VERIFIED))
                ? Boolean.parseBoolean(attributes.get(OpenIdProfileAttributesSet.EMAIL_VERIFIED))
                : false;
        String phone = attributes.get(OpenIdProfileAttributesSet.PHONE_NUMBER);
        boolean phoneVerified = StringUtils.hasText(attributes.get(OpenIdProfileAttributesSet.PHONE_NUMBER_VERIFIED))
                ? Boolean.parseBoolean(attributes.get(OpenIdProfileAttributesSet.PHONE_NUMBER_VERIFIED))
                : false;

        String picture = attributes.get(OpenIdProfileAttributesSet.PICTURE);
        String profile = attributes.get(OpenIdProfileAttributesSet.PROFILE);
        String website = attributes.get(OpenIdProfileAttributesSet.WEBSITE);
        String gender = attributes.get(OpenIdProfileAttributesSet.GENDER);

        String lang = attributes.get(OpenIdProfileAttributesSet.LOCALE);

        Date birthdate = null;
        if (StringUtils.hasText(attributes.get(OpenIdProfileAttributesSet.BIRTHDATE))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                birthdate = sdf.parse(attributes.get(OpenIdProfileAttributesSet.BIRTHDATE));
            } catch (Exception e) {
                // ignore
            }
        }

        // set in profile
        openIdAttributeSet.setPreferredUsername(username);
        openIdAttributeSet.setName(name);
        openIdAttributeSet.setFamilyName(familyName);
        openIdAttributeSet.setGivenName(givenName);
        if (StringUtils.hasText(email)) {
            openIdAttributeSet.setEmail(email);
            openIdAttributeSet.setEmailVerified(emailVerified);
        }
        if (StringUtils.hasText(phone)) {
            openIdAttributeSet.setPhoneNumber(phone);
            openIdAttributeSet.setPhoneVerified(phoneVerified);
        }
        openIdAttributeSet.setPicture(picture);
        openIdAttributeSet.setProfile(profile);
        openIdAttributeSet.setWebsite(website);
        openIdAttributeSet.setGender(gender);
        openIdAttributeSet.setLocale(lang);
        openIdAttributeSet.setBirthdate(birthdate);

        // we override these every time
        account.setUsername(username);
        account.setName(name);
        account.setFamilyName(familyName);
        account.setGivenName(givenName);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setPictureUri(picture);
        account.setProfileUri(profile);
        account.setLang(lang);

        account = accountRepository.saveAndFlush(account);

        // update additional attributes in store, remove stale
        Set<Entry<String, String>> userAttributes = attributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(JWT_ATTRIBUTES, e.getKey()) &&
                        !ArrayUtils.contains(ACCOUNT_ATTRIBUTES, e.getKey()))
                .collect(Collectors.toSet());
        // build an additional attributeSet for additional attributes, specific for this
        // provider
        // TODO build via attribute provider and record fields to keep an attributeSet
        // model
        DefaultUserAttributesImpl attributeSet = new DefaultUserAttributesImpl(getAuthority(), getProvider(),
                getRealm(),
                providerName);
        attributeSet.setUserId(exportInternalId(userId));

        Set<Entry<String, Serializable>> storeAttributes = new HashSet<>();
        for (Entry<String, String> e : userAttributes) {
            Entry<String, Serializable> es = new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue());
            storeAttributes.add(es);

            attributeSet.addAttribute(new StringAttribute(e.getKey(), e.getValue()));

        }

        attributeStore.setAttributes(userId, storeAttributes);

        // build identity
        // detach account
        account = accountRepository.detach(account);

        // export userId
        account.setUserId(exportInternalId(userId));

        List<UserAttributes> identityAttributes = new ArrayList<>();
        identityAttributes.add(openIdAttributeSet);
        identityAttributes.add(attributeSet);

        // write custom model
        OIDCUserIdentity identity = new OIDCUserIdentity(getProvider(), getRealm(), user);
        identity.setAccount(account);
        identity.setAttributes(identityAttributes);

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserIdentity getIdentity(String subject, String userId) throws NoSuchUserException {
        OIDCUserAccount account = accountProvider.getAccount(userId);

        if (!account.getSubject().equals(subject)) {
            throw new NoSuchUserException();
        }

        // we build only openid profile, it supersedes basic
        OpenIdProfileAttributesSet openIdAttributeSet = new OpenIdProfileAttributesSet(SystemKeys.AUTHORITY_OIDC,
                getProvider(), getRealm(), userId);

        // set only account properties
        openIdAttributeSet.setPreferredUsername(account.getUsername());
        openIdAttributeSet.setName(account.getName());
        openIdAttributeSet.setFamilyName(account.getFamilyName());
        openIdAttributeSet.setGivenName(account.getGivenName());
        openIdAttributeSet.setEmail(account.getEmail());
        openIdAttributeSet.setEmailVerified(account.getEmailVerified());
        openIdAttributeSet.setPicture(account.getPictureUri());
        openIdAttributeSet.setProfile(account.getProfileUri());
        openIdAttributeSet.setLocale(account.getLang());

        // write custom model
        OIDCUserIdentity identity = new OIDCUserIdentity(getProvider(), getRealm());
        identity.setAccount(account);
        identity.setAttributes(Collections.singleton(openIdAttributeSet));
        return identity;

    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserIdentity getIdentity(String subject, String userId, boolean fetchAttributes)
            throws NoSuchUserException {
        OIDCUserIdentity identity = getIdentity(subject, userId);

        if (fetchAttributes) {
            // load all attributes from store and map
            // TODO integrate openId set
            // TODO build additional set
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserIdentity> listIdentities(String subject) {
        // TODO handle not persisted configuration
        List<UserIdentity> identities = new ArrayList<>();

        Collection<OIDCUserAccount> accounts = accountProvider.listAccounts(subject);

        for (OIDCUserAccount account : accounts) {
            // write custom model
            OIDCUserIdentity identity = new OIDCUserIdentity(getProvider(), getRealm());
            identity.setAccount(account);
            identity.setAttributes(Collections.emptyList());
            identities.add(identity);
        }

        return identities;

    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return OIDCIdentityAuthority.AUTHORITY_URL + "authorize/" + getProvider();
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        // we don't have one
        return null;
    }

    @Override
    public boolean canRegister() {
        return false;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    @Override
    public AccountService getAccountService() {
        // TODO implement a delete-only accountService
        return null;
    }

    @Override
    public CredentialsService getCredentialsService() {
        // nothing to handle
        return null;
    }

    @Override
    @Transactional(readOnly = false)
    public UserIdentity registerIdentity(
            String subject, UserAccount account,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        throw new RegistrationException("registration not supported");
    }

    @Override
    @Transactional(readOnly = false)
    public UserIdentity updateIdentity(String subject,
            String userId, UserAccount account,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        throw new RegistrationException("update not supported");

    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String subjectId, String userId) throws NoSuchUserException {
        // TODO delete via service

    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String subjectId) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getRegistrationUrl() {
        return null;
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
    public ConfigurableProperties getConfiguration() {
        return providerConfig;
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
            OpenIdProfileAttributesSet.NAME,
            OpenIdProfileAttributesSet.FAMILY_NAME,
            OpenIdProfileAttributesSet.GIVEN_NAME,
            OpenIdProfileAttributesSet.EMAIL,
            OpenIdProfileAttributesSet.EMAIL_VERIFIED,
            OpenIdProfileAttributesSet.PICTURE,
            OpenIdProfileAttributesSet.LOCALE
    };

//    private final static ObjectMapper mapper = new ObjectMapper();
//    private final static TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<HashMap<String, Serializable>>() {
//    };
//    private final static TypeReference<HashMap<String, String>> stringMapTypeRef = new TypeReference<HashMap<String, String>>() {
//    };
}
