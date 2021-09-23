package it.smartcommunitylab.aac.spid.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.SpidUserIdentity;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountRepository;

public class SpidIdentityProvider extends AbstractProvider implements IdentityProvider {

    private final SpidUserAccountRepository accountRepository;

    private final SpidIdentityProviderConfig providerConfig;

    // internal providers
    private final SpidAccountProvider accountProvider;
    private final SpidAttributeProvider attributeProvider;
    private final SpidAuthenticationProvider authenticationProvider;
    private final SpidSubjectResolver subjectResolver;

    private ScriptExecutionService executionService;

    public SpidIdentityProvider(
            String providerId, String providerName,
            SpidUserAccountRepository accountRepository,
            SpidIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        // internal data repositories
        this.accountRepository = accountRepository;

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");
        this.providerConfig = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new SpidAccountProvider(providerId, accountRepository, config, realm);
        this.attributeProvider = new SpidAttributeProvider(providerId, accountRepository, config, realm);
        this.authenticationProvider = new SpidAuthenticationProvider(providerId, config, realm);
        this.subjectResolver = new SpidSubjectResolver(providerId, accountRepository, config, realm);

    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
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

    @Override
    public ExtendedAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public SpidAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public SpidAttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public SpidSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    @Transactional(readOnly = false)
    public UserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException {
        // we expect an instance of our model
        SpidAuthenticatedPrincipal user = (SpidAuthenticatedPrincipal) principal;
        // we use internal id for accounts
        // NOTE: spid nameId is transient, so each login will result in a new
        // registration, unless provider is configured to use spidCode as userId
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
        SpidUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);

        if (account == null) {

            account = new SpidUserAccount();
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

        // DISABLED custom mapping for SPID
        // TODO evaluate support
//        // let hook process custom mapping
//        if (executionService != null && providerConfig.getHookFunctions() != null
//                && StringUtils.hasText(providerConfig.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION))) {
//
//            try {
//                // execute script
//                String functionCode = providerConfig.getHookFunctions().get(ATTRIBUTE_MAPPING_FUNCTION);
//                Map<String, Serializable> customAttributes = executionService.executeFunction(
//                        ATTRIBUTE_MAPPING_FUNCTION,
//                        functionCode, principalAttributes);
//
//                // update map
//                if (customAttributes != null) {
//                    // replace map
//                    principalAttributes = customAttributes;
//
//                    // TODO handle non string
//                    attributes = customAttributes.entrySet().stream()
//                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));
//                }
//
//            } catch (SystemException | InvalidDefinitionException ex) {
////                logger.error(ex.getMessage());
//            }
//
//        }

        // update account attributes
        // fetch from principal attributes - exact match only
        String username = user.getName();
        String name = attributes.get(SpidAttribute.NAME.getValue());
        String surname = attributes.get(SpidAttribute.FAMILY_NAME.getValue());
        String email = attributes.get(SpidAttribute.EMAIL.getValue());

        // we override every time
        account.setUsername(username);
        account.setName(name);
        account.setSurname(surname);
        account.setEmail(email);

        account = accountRepository.saveAndFlush(account);

        // attributes are not persisted as default policy
        // TODO evaluate an in-memory,per-session attribute store

        // build identity
        // detach account
        account = accountRepository.detach(account);

        // export userId
        account.setUserId(exportInternalId(userId));

        // convert attribute sets
        Collection<UserAttributes> identityAttributes = attributeProvider.convertAttributes(user, subjectId);

        // write custom model
        SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm(), user);
        identity.setAccount(account);
        identity.setAttributes(identityAttributes);

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public SpidUserIdentity getIdentity(String subject, String userId) throws NoSuchUserException {
        return getIdentity(subject, userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public SpidUserIdentity getIdentity(String subject, String userId, boolean fetchAttributes)
            throws NoSuchUserException {
        SpidUserAccount account = accountProvider.getAccount(userId);

        if (!account.getSubject().equals(subject)) {
            throw new NoSuchUserException();
        }

        // write custom model
        SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm());
        identity.setAccount(account);

        if (fetchAttributes) {
            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAttributes(userId);
            identity.setAttributes(identityAttributes);
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SpidUserIdentity> listIdentities(String subject) {
        return listIdentities(subject, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SpidUserIdentity> listIdentities(String subject, boolean fetchAttributes) {
        // TODO handle not persisted configuration
        List<SpidUserIdentity> identities = new ArrayList<>();

        Collection<SpidUserAccount> accounts = accountProvider.listAccounts(subject);

        for (SpidUserAccount account : accounts) {
            // write custom model
            SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm());
            identity.setAccount(account);

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
        // attributes are not persisted as default policy
        // TODO evaluate an in-memory,per-session attribute store

    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String subjectId) {
        Collection<SpidUserAccount> accounts = accountProvider.listAccounts(subjectId);
        for (SpidUserAccount account : accounts) {
            try {
                deleteIdentity(subjectId, account.getUserId());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return SpidIdentityAuthority.AUTHORITY_URL
                + "authenticate/" + getProvider();
    }

//    @Override
//    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
//        // we don't have one
//        return null;
//    }

    public static String[] SAML_ATTRIBUTES = {
            "subject", "issuer", "issueInstant"
    };

    public static String[] ACCOUNT_ATTRIBUTES = {
            "username",
            "name",
            "email",
            "locale"
    };

    @Override
    public String getDisplayMode() {
        // not configurable
        return SystemKeys.DISPLAY_MODE_SPID;
    }

    @Override
    public Map<String, String> getActionUrls() {
        return Collections.singletonMap(SystemKeys.ACTION_LOGIN, getAuthenticationUrl());
    }

}
