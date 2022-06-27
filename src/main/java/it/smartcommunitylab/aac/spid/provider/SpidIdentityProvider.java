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
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.dto.LoginProvider;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.attributes.SpidAttributesMapper;
import it.smartcommunitylab.aac.spid.attributes.SpidAttributesSet;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.model.SpidUserIdentity;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountRepository;

public class SpidIdentityProvider extends AbstractProvider
        implements
        IdentityProvider<SpidUserIdentity> {

    // provider configuration
    private final SpidIdentityProviderConfig config;

    // internal providers
    private final SpidAccountProvider accountProvider;
    private final SpidAttributeProvider attributeProvider;
    private final SpidAuthenticationProvider authenticationProvider;
    private final SpidSubjectResolver subjectResolver;

    // attributes
    private final SpidAttributesMapper spidMapper;

    public SpidIdentityProvider(
            String providerId, String providerName,
            SpidUserAccountRepository accountRepository, SubjectService subjectService,
            SpidIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");
        this.config = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new SpidAccountProvider(providerId, accountRepository, subjectService, config, realm);
        this.attributeProvider = new SpidAttributeProvider(providerId, config, realm);
        this.authenticationProvider = new SpidAuthenticationProvider(providerId, config, realm);
        this.subjectResolver = new SpidSubjectResolver(providerId, accountRepository, config, realm);

        this.spidMapper = new SpidAttributesMapper();

    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public SpidIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public SpidAuthenticationProvider getAuthenticationProvider() {
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
    public SpidUserIdentity convertIdentity(UserAuthenticatedPrincipal userPrincipal, String userId)
            throws NoSuchUserException {
        // we expect an instance of our model
        Assert.isInstanceOf(SpidAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of internal authenticated principal");
        SpidAuthenticatedPrincipal principal = (SpidAuthenticatedPrincipal) userPrincipal;

        // we use upstream subjectId for accounts
        // NOTE: spid nameId is transient, so each login will result in a new
        // registration, unless provider is configured to use spidCode as userId
        String subjectId = principal.getSubjectId();

        // attributes from provider
        String username = principal.getUsername();
        Map<String, Serializable> attributes = principal.getAttributes();

        if (userId == null) {
            // this better exists
            throw new NoSuchUserException();
        }

        // get all attributes from principal except saml attrs
        // TODO handle all attributes not only strings.
        Map<String, Serializable> principalAttributes = attributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(SAML_ATTRIBUTES, e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        // TODO evaluate hook custom mapping
        // re-read attributes as-is, transform to strings
        // map attributes to spid set and flatten to string
        AttributeSet spidAttributeSet = spidMapper.mapAttributes(principalAttributes);
        Map<String, String> spidAttributes = spidAttributeSet.getAttributes()
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getKey(),
                        a -> a.exportValue()));

        String email = spidAttributes.get(SpidAttributesSet.EMAIL);
        String phone = spidAttributes.get(SpidAttributesSet.MOBILE_PHONE);
        String spidCode = spidAttributes.get(SpidAttributesSet.SPID_CODE);
        String fiscalNumber = spidAttributes.get(SpidAttributesSet.FISCAL_NUMBER);
        String ivaCode = spidAttributes.get(SpidAttributesSet.IVA_CODE);

        // TODO handle not persisted configuration
        //
        // look in repo or create
        SpidUserAccount account = accountProvider.findAccount(subjectId);

        if (account == null) {

            account = new SpidUserAccount();
            account.setSubjectId(subjectId);
            account.setUsername(username);
            account.setEmail(email);
            account.setPhone(phone);
            account.setSpidCode(spidCode);
            account.setFiscalNumber(fiscalNumber);
            account.setIvaCode(ivaCode);
            account = accountProvider.registerAccount(userId, account);
        }

        // uuid is available for persisted accounts
        String uuid = account.getUuid();
        principal.setUuid(uuid);

        // userId is always present, is derived from the same account table
        String curUserId = account.getUserId();

        if (!curUserId.equals(userId)) {
//            // force link
//            // TODO re-evaluate
//            account.setSubject(subjectId);
//            account = accountRepository.save(account);
            throw new IllegalArgumentException("user mismatch");
        }

        // issuer is idp id
        String issuer = spidAttributes.get("issuer");

        // update account attributes
        String name = spidAttributes.get(SpidAttributesSet.NAME);
        String surname = spidAttributes.get(SpidAttributesSet.FAMILY_NAME);

        // we override these every time
        account.setUsername(username);
        account.setIdp(issuer);
        account.setEmail(email);
        account.setPhone(phone);
        account.setSpidCode(spidCode);
        account.setFiscalNumber(fiscalNumber);
        account.setIvaCode(ivaCode);
        account.setName(name);
        account.setSurname(surname);

        account = accountProvider.updateAccount(subjectId, account);

        // convert attribute sets via provider
        // attributes are not persisted as default policy
        // TODO evaluate an in-memory,per-session attribute store
        Collection<UserAttributes> identityAttributes = attributeProvider.convertPrincipalAttributes(principal,
                account);

        // build identity
        SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm(), account, principal);
        identity.setAttributes(identityAttributes);

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public SpidUserIdentity findIdentityByUuid(String uuid) {
        // lookup a matching account
        SpidUserAccount account = accountProvider.findAccountByUuid(uuid);
        if (account == null) {
            return null;
        }
        // build identity without attributes
        SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm(), account);
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public SpidUserIdentity findIdentity(String subjectId) {
        // lookup a matching account
        SpidUserAccount account = accountProvider.findAccount(subjectId);
        if (account == null) {
            return null;
        }
        // build identity without attributes
        SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm(), account);
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public SpidUserIdentity getIdentity(String subjectId) throws NoSuchUserException {
        return getIdentity(subjectId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public SpidUserIdentity getIdentity(String subjectId, boolean fetchAttributes)
            throws NoSuchUserException {
        // lookup a matching account
        SpidUserAccount account = accountProvider.getAccount(subjectId);

        // build identity
        SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm(), account);
        if (fetchAttributes) {
            // convert attribute sets
            Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
            identity.setAttributes(identityAttributes);
        }

        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SpidUserIdentity> listIdentities(String userId) {
        return listIdentities(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SpidUserIdentity> listIdentities(String userId, boolean fetchAttributes) {
        // TODO handle not persisted configuration
        // lookup for matching accounts
        List<SpidUserAccount> accounts = accountProvider.listAccounts(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<SpidUserIdentity> identities = new ArrayList<>();

        for (SpidUserAccount account : accounts) {
            // build identity
            SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm(), account);
            if (fetchAttributes) {
                // convert attribute sets
                Collection<UserAttributes> identityAttributes = attributeProvider.getAccountAttributes(account);
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
        // attributes are not persisted as default policy
        // TODO evaluate an in-memory,per-session attribute store
        attributeProvider.deleteAccountAttributes(subjectId);

        // delete account
        accountProvider.deleteAccount(subjectId);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String userId) {
        Collection<SpidUserAccount> accounts = accountProvider.listAccounts(userId);
        for (SpidUserAccount account : accounts) {
            try {
                deleteIdentity(account.getSubjectId());
            } catch (NoSuchUserException e) {
            }
        }
    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return SpidIdentityAuthority.AUTHORITY_URL + "authenticate/" + getProvider();
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
    public LoginProvider getLoginProvider() {
        LoginProvider lp = new LoginProvider(SystemKeys.AUTHORITY_SPID, getProvider(), getRealm());
        lp.setName(getName());
        lp.setDescription(getDescription());

        lp.setLoginUrl(getAuthenticationUrl());
        lp.setTemplate("spid");

        lp.setConfiguration(getConfig());

        return lp;
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
