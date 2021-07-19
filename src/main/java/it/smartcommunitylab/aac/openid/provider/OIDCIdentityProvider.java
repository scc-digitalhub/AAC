package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
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
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.AttributeManager;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.DefaultAttributesMapper;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
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
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.CredentialsService;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.openid.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCIdentityProvider extends AbstractProvider implements IdentityService {

    private final String providerName;

    // services
    private final OIDCUserAccountRepository accountRepository;
    private final AttributeStore attributeStore;

    private final OIDCIdentityProviderConfig providerConfig;

    // internal providers
    private final OIDCAccountProvider accountProvider;
//    private final OIDCAttributeProvider attributeProvider;
    private final OIDCAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

    // attributes
    private final OpenIdAttributesMapper openidMapper;
    private ScriptExecutionService executionService;
    private AttributeManager attributeService;

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
        this.accountProvider = new OIDCAccountProvider(providerId, accountRepository, config, realm);
//        this.attributeProvider = new OIDCAttributeProvider(providerId, accountRepository, attributeStore, realm);
        this.authenticationProvider = new OIDCAuthenticationProvider(providerId, accountRepository, config, realm);
        this.subjectResolver = new OIDCSubjectResolver(providerId, accountRepository, config, realm);

        // attributes
        openidMapper = new OpenIdAttributesMapper();
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    // TODO remove and move to attributeProvider
    public void setAttributeService(AttributeManager attributeManager) {
        this.attributeService = attributeManager;
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

//    @Override
//    public AttributeProvider getAttributeProvider() {
//        return attributeProvider;
//    }

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
        // we use upstream id for accounts
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
                if (customAttributes != null) {
                    // replace map
                    principalAttributes = customAttributes;

                    // TODO handle non string
                    attributes = customAttributes.entrySet().stream()
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));
                }
            } catch (SystemException | InvalidDefinitionException ex) {
//                logger.error(ex.getMessage());
            }

        }

        // update account attributes
        // fetch from principal attributes - exact match only
        String username = user.getName();
        String name = attributes.get(OpenIdAttributesSet.NAME);
        String familyName = attributes.get(OpenIdAttributesSet.FAMILY_NAME);
        String givenName = attributes.get(OpenIdAttributesSet.GIVEN_NAME);
        String email = attributes.get(OpenIdAttributesSet.EMAIL);
        boolean emailVerified = StringUtils.hasText(attributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                ? Boolean.parseBoolean(attributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                : false;

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
                .filter(e -> !ArrayUtils.contains(JWT_ATTRIBUTES, e.getKey()) &&
                        !ArrayUtils.contains(ACCOUNT_ATTRIBUTES, e.getKey()))
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
        List<UserAttributes> identityAttributes = extractUserAttributes(account, principalAttributes);

        // write custom model
        OIDCUserIdentity identity = new OIDCUserIdentity(getProvider(), getRealm(), user);
        identity.setAccount(account);
        identity.setAttributes(identityAttributes);

        return identity;
    }

    // TODO move to (idp) attributeProvider
    private List<UserAttributes> extractUserAttributes(OIDCUserAccount account,
            Map<String, Serializable> principalAttributes) {
        List<UserAttributes> attributes = new ArrayList<>();
        String userId = exportInternalId(account.getUserId());

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        String name = account.getName() != null ? account.getName() : account.getGivenName();
        basicset.setName(name);
        basicset.setSurname(account.getFamilyName());
        basicset.setEmail(account.getEmail());
        basicset.setUsername(account.getUsername());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                basicset));

        // account
        AccountAttributesSet accountset = new AccountAttributesSet();
        accountset.setUsername(account.getUsername());
        accountset.setUserId(account.getUserId());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                accountset));
        // email
        EmailAttributesSet emailset = new EmailAttributesSet();
        emailset.setEmail(account.getEmail());
        emailset.setEmailVerified(account.getEmailVerified());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                emailset));

        if (principalAttributes != null) {
            // openid via mapper
            AttributeSet openidset = openidMapper.mapAttributes(principalAttributes);
            attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                    openidset));

            // build an additional attributeSet for additional attributes, specific for this
            // provider
            // TODO build via attribute provider and record fields to keep an attributeSet
            // model
            DefaultUserAttributesImpl idpset = new DefaultUserAttributesImpl(getAuthority(), getProvider(),
                    getRealm(), userId, "idp." + providerName);
            // store everything as string
            for (Map.Entry<String, Serializable> e : principalAttributes.entrySet()) {
                try {
                    idpset.addAttribute(new StringAttribute(e.getKey(), StringAttribute.parseValue(e.getValue())));
                } catch (ParseException e1) {
                }
            }
            attributes.add(idpset);

            // build additional user-defined attribute sets via mappers
            if (attributeService != null) {
                Collection<AttributeSet> sets = attributeService.listAttributeSets(getRealm());
                for (AttributeSet as : sets) {
                    DefaultAttributesMapper amap = new DefaultAttributesMapper(as);
                    AttributeSet set = amap.mapAttributes(principalAttributes);
                    if (set.getAttributes() != null && !set.getAttributes().isEmpty()) {
                        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                                set));
                    }
                }

            }

        }

        return attributes;
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

        if (fetchAttributes) {
            // fetch stored principal attributes if present
            String id = parseResourceId(userId);
            Map<String, Serializable> principalAttributes = attributeStore.findAttributes(id);

            // convert attribute sets
            List<UserAttributes> identityAttributes = extractUserAttributes(account, principalAttributes);
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

            if (fetchAttributes) {
                // fetch stored principal attributes if present
                String id = parseResourceId(account.getUserId());
                Map<String, Serializable> principalAttributes = attributeStore.findAttributes(id);

                // convert attribute sets
                List<UserAttributes> identityAttributes = extractUserAttributes(account, principalAttributes);
                identity.setAttributes(identityAttributes);
            }

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
            OpenIdAttributesSet.NAME,
            OpenIdAttributesSet.FAMILY_NAME,
            OpenIdAttributesSet.GIVEN_NAME,
            OpenIdAttributesSet.EMAIL,
            OpenIdAttributesSet.EMAIL_VERIFIED,
            OpenIdAttributesSet.PICTURE,
            OpenIdAttributesSet.LOCALE
    };

//    private final static ObjectMapper mapper = new ObjectMapper();
//    private final static TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<HashMap<String, Serializable>>() {
//    };
//    private final static TypeReference<HashMap<String, String>> stringMapTypeRef = new TypeReference<HashMap<String, String>>() {
//    };
}
