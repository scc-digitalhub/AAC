package it.smartcommunitylab.aac.saml.provider;

import java.io.Serializable;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
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
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;
import it.smartcommunitylab.aac.saml.SamlUserIdentity;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

public class SamlIdentityProvider extends AbstractProvider implements IdentityService {

    private final String providerName;

    // services
    private final SamlUserAccountRepository accountRepository;
    private final AttributeStore attributeStore;

    private final SamlIdentityProviderConfig providerConfig;

    // internal providers
    private final SamlAccountProvider accountProvider;
    private final SamlAttributeProvider attributeProvider;
    private final SamlAuthenticationProvider authenticationProvider;
    private final SamlSubjectResolver subjectResolver;

    // attributes
    private final OpenIdAttributesMapper openidMapper;
    private ScriptExecutionService executionService;

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    public SamlIdentityProvider(
            String providerId, String providerName,
            SamlUserAccountRepository accountRepository, AttributeStore attributeStore,
            SamlIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
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
        this.accountProvider = new SamlAccountProvider(providerId, accountRepository, config, realm);
        this.attributeProvider = new SamlAttributeProvider(providerId, accountRepository, attributeStore, realm);
        this.authenticationProvider = new SamlAuthenticationProvider(providerId, accountRepository, config, realm);
        this.subjectResolver = new SamlSubjectResolver(providerId, accountRepository, config, realm);

        // attributes
        openidMapper = new OpenIdAttributesMapper();
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
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
    public SamlUserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException {
        // we expect an instance of our model
        SamlAuthenticatedPrincipal user = (SamlAuthenticatedPrincipal) principal;
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
        SamlUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);

        if (account == null) {

            account = new SamlUserAccount();
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

        String issuer = attributes.get("issuer");
        if (!StringUtils.hasText(issuer)) {
            issuer = provider;
        }
        account.setIssuer(issuer);

        // get all attributes from principal except message attrs
        // TODO handle all attributes not only strings.
        Map<String, Serializable> principalAttributes = attributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(SAML_ATTRIBUTES, e.getKey()))
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
        String name = attributes.get("name");
        String email = attributes.get("email");
        String lang = attributes.get("locale");

        // we override every time
        account.setUsername(username);
        account.setName(name);
        account.setEmail(email);
        account.setLang(lang);

        account = accountRepository.saveAndFlush(account);

        // update additional attributes in store, remove stale
        Set<Entry<String, Serializable>> userAttributes = principalAttributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(SAML_ATTRIBUTES, e.getKey()) &&
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
        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), user);
        identity.setAccount(account);
        identity.setAttributes(identityAttributes);

        return identity;
    }

    private List<UserAttributes> extractUserAttributes(SamlUserAccount account,
            Map<String, Serializable> principalAttributes) {
        List<UserAttributes> attributes = new ArrayList<>();
        String userId = exportInternalId(account.getUserId());

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        String name = account.getName() != null ? account.getName() : account.getUsername();
        basicset.setName(name);
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

            // TODO build additional user-defined attribute sets via mappers

        }

        return attributes;
    }

    @Override
    @Transactional(readOnly = true)
    public SamlUserIdentity getIdentity(String subject, String userId) throws NoSuchUserException {
        SamlUserAccount account = accountProvider.getAccount(userId);

        if (!account.getSubject().equals(subject)) {
            throw new NoSuchUserException();
        }

        // fetch stored principal attributes if present
        String id = parseResourceId(userId);
        Map<String, Serializable> principalAttributes = attributeStore.findAttributes(id);

        // convert attribute sets
        List<UserAttributes> identityAttributes = extractUserAttributes(account, principalAttributes);

        // write custom model
        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm());
        identity.setAccount(account);
        identity.setAttributes(identityAttributes);
        return identity;

    }

    @Override
    @Transactional(readOnly = true)
    public SamlUserIdentity getIdentity(String subject, String userId, boolean fetchAttributes)
            throws NoSuchUserException {
        if (fetchAttributes) {
            return getIdentity(subject, userId);
        }

        SamlUserAccount account = accountProvider.getAccount(userId);

        if (!account.getSubject().equals(subject)) {
            throw new NoSuchUserException();
        }

        // write custom model
        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm());
        identity.setAccount(account);
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserIdentity> listIdentities(String subject) {
        // TODO handle not persisted configuration
        List<UserIdentity> identities = new ArrayList<>();

        Collection<SamlUserAccount> accounts = accountProvider.listAccounts(subject);

        for (SamlUserAccount account : accounts) {
            // write custom model
            SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm());
            identity.setAccount(account);
            identity.setAttributes(Collections.emptyList());
            identities.add(identity);
        }

        return identities;

    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return SamlIdentityAuthority.AUTHORITY_URL
                + "authenticate/" + getProvider();
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

    public static String[] SAML_ATTRIBUTES = {
            "subject", "issuer", "issueInstant"
    };

    public static String[] ACCOUNT_ATTRIBUTES = {
            "username",
            "name",
            "email",
            "locale"
    };
}
