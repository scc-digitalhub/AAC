package it.smartcommunitylab.aac.spid.provider;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.AttributeManager;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.DefaultAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.SpidUserIdentity;
import it.smartcommunitylab.aac.spid.attributes.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.spid.attributes.SpidAttributesMapper;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountRepository;

public class SpidIdentityProvider extends AbstractProvider implements IdentityProvider {

    private final String providerName;

    private final SpidUserAccountRepository accountRepository;

    private final SpidIdentityProviderConfig providerConfig;

    // internal providers
    private final SpidAccountProvider accountProvider;
    private final SpidAuthenticationProvider authenticationProvider;
    private final SpidSubjectResolver subjectResolver;

    // attributes
    private final OpenIdAttributesMapper openidMapper;
    private final SpidAttributesMapper spidMapper;
    private ScriptExecutionService executionService;
    private AttributeManager attributeService;

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    public SpidIdentityProvider(
            String providerId, String providerName,
            SpidUserAccountRepository accountRepository,
            SpidIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.providerName = StringUtils.hasText(providerName) ? providerName : providerId;

        // internal data repositories
        this.accountRepository = accountRepository;

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");
        this.providerConfig = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new SpidAccountProvider(providerId, accountRepository, config, realm);
        this.authenticationProvider = new SpidAuthenticationProvider(providerId, config, realm);
        this.subjectResolver = new SpidSubjectResolver(providerId, accountRepository, config, realm);

        // attributes
        openidMapper = new OpenIdAttributesMapper();
        spidMapper = new SpidAttributesMapper();
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    // TODO remove and move to attributeProvider
    public void setAttributeService(AttributeManager attributeManager) {
        this.attributeService = attributeManager;
    }

    @Override
    public ExtendedAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
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
    public AccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public SubjectResolver getSubjectResolver() {
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
        List<UserAttributes> identityAttributes = extractUserAttributes(account, principalAttributes);

        // write custom model
        SpidUserIdentity identity = new SpidUserIdentity(getProvider(), getRealm(), user);
        identity.setAccount(account);
        identity.setAttributes(identityAttributes);

        return identity;
    }

    // TODO move to attributeProvider
    private List<UserAttributes> extractUserAttributes(SpidUserAccount account,
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
            // spid via mapper
            AttributeSet spidset = spidMapper.mapAttributes(principalAttributes);
            attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                    spidset));

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
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return SpidIdentityAuthority.AUTHORITY_URL
                + "authenticate/" + getProvider();
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        // we don't have one
        return null;
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

    @Override
    public String getLoginComponent() {
        return "login/spid";
    }

}
