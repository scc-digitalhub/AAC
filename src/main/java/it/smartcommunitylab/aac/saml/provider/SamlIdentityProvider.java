package it.smartcommunitylab.aac.saml.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

public class SamlIdentityProvider extends AbstractProvider implements IdentityProvider {
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
            SamlUserAccountRepository accountRepository, AttributeStore attributeStore,
            SamlIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(attributeStore, "attribute store is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");

        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new SamlAccountProvider(providerId, accountRepository, config, realm);
        this.attributeProvider = new SamlAttributeProvider(providerId, accountRepository, attributeStore, config,
                realm);
        this.authenticationProvider = new SamlAuthenticationProvider(providerId, accountRepository, config, realm);
        this.subjectResolver = new SamlSubjectResolver(providerId, accountRepository, config, realm);

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
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
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
    @Transactional(readOnly = false)
    public SamlUserIdentity convertIdentity(UserAuthenticatedPrincipal userPrincipal, String userId)
            throws NoSuchUserException {
        // we expect an instance of our model
        Assert.isInstanceOf(SamlUserAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of internal authenticated principal");
        SamlUserAuthenticatedPrincipal principal = (SamlUserAuthenticatedPrincipal) userPrincipal;

        // we use upstream subject for accounts
        // TODO handle transient ids, for example with session persistence
        String subjectId = principal.getSubjectId();
        String provider = getProvider();

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // attributes from provider
        String username = principal.getUsername();
        Map<String, Serializable> attributes = principal.getAttributes();

        // re-read attributes as-is, transform to strings
        // TODO evaluate using a custom mapper to given profile
        Map<String, String> samlAttributes = attributes.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

        String email = samlAttributes.get("email");
        username = StringUtils.hasText(samlAttributes.get("username"))
                ? samlAttributes.get("username")
                : principal.getUsername();

        principal.setUsername(username);
        principal.setEmail(email);

        // TODO handle not persisted configuration
        //
        // look in repo or create
        SamlUserAccount account = accountProvider.findAccount(subjectId);

        if (account == null) {
            // create
            account = new SamlUserAccount();
            account.setSubjectId(subjectId);
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
        String issuer = samlAttributes.get("issuer");
        if (!StringUtils.hasText(issuer)) {
            issuer = config.getRelyingPartyRegistration().getAssertingPartyDetails().getEntityId();
        }

        String name = StringUtils.hasText(samlAttributes.get("name")) ? samlAttributes.get("name") : username;

        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
                ? config.getConfigMap().getTrustEmailAddress()
                : false;
        boolean emailVerified = StringUtils.hasText(samlAttributes.get("emailVerified"))
                ? Boolean.parseBoolean(samlAttributes.get("emailVerified"))
                : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }
        principal.setEmailVerified(emailVerified);

        // we override these every time
        account.setIssuer(issuer);
        account.setUsername(username);
        account.setName(name);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setLang(null);

        account = accountProvider.updateAccount(subjectId, account);

        // convert attribute sets via provider, will update store
        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal, userId);

        // build identity
        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account, principal);
        identity.setAttributes(identityAttributes);

        return identity;

    }

    @Override
    @Transactional(readOnly = true)
    public SamlUserIdentity getIdentity(String subjectId) throws NoSuchUserException {
        return getIdentity(subjectId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public SamlUserIdentity getIdentity(String subjectId, boolean fetchAttributes)
            throws NoSuchUserException {
        // lookup a matching account
        SamlUserAccount account = accountProvider.getAccount(subjectId);

        // build identity
        SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account);
        if (fetchAttributes) {
            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(subjectId);
            identity.setAttributes(identityAttributes);
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SamlUserIdentity> listIdentities(String userId) {
        return listIdentities(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SamlUserIdentity> listIdentities(String userId, boolean fetchAttributes) {
        // TODO handle not persisted configuration
        // lookup for matching accounts
        List<SamlUserAccount> accounts = accountProvider.listAccounts(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<SamlUserIdentity> identities = new ArrayList<>();

        for (SamlUserAccount account : accounts) {
            // build identity
            SamlUserIdentity identity = new SamlUserIdentity(getProvider(), getRealm(), account);
            if (fetchAttributes) {
                // convert attribute sets
                Collection<UserAttributes> identityAttributes = attributeProvider
                        .getAccountAttributes(account.getSubjectId());
                identity.setAttributes(identityAttributes);
            }

            identities.add(identity);
        }

        return identities;
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String subjectId) throws NoSuchUserException {
        // cleanup attributes
        attributeProvider.deleteAccountAttributes(subjectId);

        // delete account
        accountProvider.deleteAccount(subjectId);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String userId) {
        Collection<SamlUserAccount> accounts = accountProvider.listAccounts(userId);
        for (SamlUserAccount account : accounts) {
            try {
                deleteIdentity(account.getSubjectId());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return SamlIdentityAuthority.AUTHORITY_URL + "authenticate/" + getProvider();
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
