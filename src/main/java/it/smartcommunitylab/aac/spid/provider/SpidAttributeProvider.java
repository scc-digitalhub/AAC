package it.smartcommunitylab.aac.spid.provider;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.spid.attributes.SpidAttributesMapper;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountRepository;

public class SpidAttributeProvider extends AbstractProvider implements AttributeProvider {

    // services
    private final SpidUserAccountRepository accountRepository;
    private final SpidIdentityProviderConfig providerConfig;

    private final OpenIdAttributesMapper openidMapper;
    private final SpidAttributesMapper spidMapper;

    public SpidAttributeProvider(
            String providerId,
            SpidUserAccountRepository accountRepository,
            SpidIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.accountRepository = accountRepository;
        this.providerConfig = providerConfig;

        // attributes
        openidMapper = new OpenIdAttributesMapper();
        spidMapper = new SpidAttributesMapper();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
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
    public Collection<UserAttributes> convertAttributes(UserAuthenticatedPrincipal principal, String subjectId) {
        // we expect an instance of our model
        SpidAuthenticatedPrincipal user = (SpidAuthenticatedPrincipal) principal;
        String userId = parseResourceId(user.getUserId());
        String realm = getRealm();
        String provider = getProvider();

        Map<String, String> attributes = user.getAttributes();

        SpidUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);
        if (account == null) {
            return null;
        }

        // get all attributes from principal except jwt attrs
        // TODO handle all attributes not only strings.
        Map<String, Serializable> principalAttributes = attributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(SpidIdentityProvider.SAML_ATTRIBUTES, e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        return extractUserAttributes(account, principalAttributes);

    }

    @Override
    public Collection<UserAttributes> getAttributes(String subjectId) {
        // we expect subjectId to be == userId
        // attributes are not persisted as default policy
        // TODO evaluate an in-memory,per-session attribute store
        return Collections.emptyList();
    }

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
                    getRealm(), userId, "idp." + getProvider());
            // store everything as string
            for (Map.Entry<String, Serializable> e : principalAttributes.entrySet()) {
                try {
                    idpset.addAttribute(new StringAttribute(e.getKey(), StringAttribute.parseValue(e.getValue())));
                } catch (ParseException e1) {
                }
            }
            attributes.add(idpset);
        }

        return attributes;
    }

    @Override
    public void deleteAttributes(String subjectId) {
        // attributes are not persisted as default policy
        // TODO evaluate an in-memory,per-session attribute store;
    }

}