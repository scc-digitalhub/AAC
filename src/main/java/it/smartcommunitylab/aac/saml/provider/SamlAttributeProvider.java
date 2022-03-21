package it.smartcommunitylab.aac.saml.provider;

import java.io.Serializable;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountId;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountId;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

public class SamlAttributeProvider extends AbstractProvider implements AttributeProvider {

    // services
    private final AttributeStore attributeStore;

    private final SamlUserAccountRepository accountRepository;
    private final SamlIdentityProviderConfig config;
    private final OpenIdAttributesMapper openidMapper;

    public SamlAttributeProvider(
            String providerId,
            SamlUserAccountRepository accountRepository, AttributeStore attributeStore,
            SamlIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        Assert.notNull(attributeStore, "attribute store is mandatory");

        this.accountRepository = accountRepository;
        this.attributeStore = attributeStore;
        this.config = providerConfig;

        // attributes
        openidMapper = new OpenIdAttributesMapper();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
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
    public Collection<UserAttributes> convertPrincipalAttributes(UserAuthenticatedPrincipal principal,
            String userId) {
        // we expect an instance of our model
        if (!(principal instanceof SamlUserAuthenticatedPrincipal)) {
            return null;
        }
        SamlUserAuthenticatedPrincipal user = (SamlUserAuthenticatedPrincipal) principal;
        String subjectId = user.getSubjectId();
        String provider = getProvider();

        Map<String, Serializable> attributes = user.getAttributes();

        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(provider, subjectId));
        if (account == null) {
            return null;
        }

        // get all attributes from principal except saml attrs
        // TODO handle all attributes not only strings.
        Map<String, Serializable> principalAttributes = attributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(SamlIdentityProvider.SAML_ATTRIBUTES, e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        Set<Entry<String, Serializable>> storeAttributes = new HashSet<>();
        for (Entry<String, Serializable> e : principalAttributes.entrySet()) {
            Entry<String, Serializable> es = new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue());
            storeAttributes.add(es);
        }

        // store attributes linked to sub
        attributeStore.setAttributes(subjectId, storeAttributes);

        return extractUserAttributes(account, principalAttributes);

    }

    @Override
    public Collection<UserAttributes> getUserAttributes(String userId) {
        // nothing is accessible here by user, only by account
        return null;
    }

    @Override
    public Collection<UserAttributes> getAccountAttributes(String subjectId) {
        String provider = getProvider();

        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(provider, subjectId));
        if (account == null) {
            return null;
        }

        // read from store
        Map<String, Serializable> principalAttributes = attributeStore.findAttributes(subjectId);
        return extractUserAttributes(account, principalAttributes);
    }

    private List<UserAttributes> extractUserAttributes(SamlUserAccount account,
            Map<String, Serializable> principalAttributes) {
        List<UserAttributes> attributes = new ArrayList<>();
        // user identifier
        String userId = account.getUserId();

        // base attributes
        String name = account.getName() != null ? account.getName() : account.getSubjectId();
        String email = account.getEmail();
        String username = account.getUsername() != null ? account.getUsername() : account.getEmail();

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        basicset.setName(name);
        basicset.setEmail(email);
        basicset.setUsername(username);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                basicset));

        // account
        AccountAttributesSet accountset = new AccountAttributesSet();
        accountset.setUsername(account.getUsername());
        accountset.setUserId(account.getUserId());
        accountset.setId(account.getSubjectId());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                accountset));
        // email
        EmailAttributesSet emailset = new EmailAttributesSet();
        emailset.setEmail(account.getEmail());
        emailset.setEmailVerified(account.getEmailVerified());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                emailset));

        // merge attributes
        Map<String, Serializable> map = new HashMap<>();
        // set store attributes
        if (principalAttributes != null) {
            map.putAll(principalAttributes);
        }

        // override from account
        map.put(OpenIdAttributesSet.NAME, name);
        map.put(OpenIdAttributesSet.EMAIL, email);
        map.put(OpenIdAttributesSet.EMAIL_VERIFIED, account.isEmailVerified());
        map.put(OpenIdAttributesSet.PREFERRED_USERNAME, username);

        if (StringUtils.hasText(account.getLang())) {
            map.put(OpenIdAttributesSet.LOCALE, account.getLang());
        }

        // openid via mapper
        AttributeSet openidset = openidMapper.mapAttributes(map);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                openidset));

        if (principalAttributes != null) {
            // build an additional attributeSet for additional attributes, specific for this
            // provider, where we export all raw attributes
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
    public void deleteUserAttributes(String userId) {
        // nothing to do
    }

    @Override
    public void deleteAccountAttributes(String subjectId) {
        // cleanup from store
        attributeStore.deleteAttributes(subjectId);
    }

}