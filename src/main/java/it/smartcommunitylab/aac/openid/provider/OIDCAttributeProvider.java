package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
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
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCAttributeProvider extends AbstractProvider implements AttributeProvider {

    // services
    private final AttributeStore attributeStore;

    private final OIDCUserAccountRepository accountRepository;
    private final OIDCIdentityProviderConfig providerConfig;
    private final OpenIdAttributesMapper openidMapper;

    public OIDCAttributeProvider(
            String providerId,
            OIDCUserAccountRepository accountRepository, AttributeStore attributeStore,
            OIDCIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_OIDC, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        Assert.notNull(attributeStore, "attribute store is mandatory");

        this.accountRepository = accountRepository;
        this.attributeStore = attributeStore;
        this.providerConfig = providerConfig;

        // attributes
        openidMapper = new OpenIdAttributesMapper();
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
        OIDCAuthenticatedPrincipal user = (OIDCAuthenticatedPrincipal) principal;
        String userId = parseResourceId(user.getUserId());
        String realm = getRealm();
        String provider = getProvider();

        Map<String, String> attributes = user.getAttributes();

        OIDCUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);
        if (account == null) {
            return null;
        }

        // get all attributes from principal except jwt attrs
        // TODO handle all attributes not only strings.
        Map<String, Serializable> principalAttributes = attributes.entrySet().stream()
                .filter(e -> !ArrayUtils.contains(OIDCIdentityProvider.JWT_ATTRIBUTES, e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        return extractUserAttributes(account, principalAttributes);

    }

    @Override
    public Collection<UserAttributes> getAttributes(String subjectId) {
        // we expect subjectId to be == userId
        String userId = subjectId;
        String realm = getRealm();
        String provider = getProvider();

        OIDCUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);
        if (account == null) {
            return null;
        }

        // read from store
        String id = parseResourceId(userId);
        Map<String, Serializable> principalAttributes = attributeStore.findAttributes(id);

        return extractUserAttributes(account, principalAttributes);
    }

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
        // cleanup from store
        attributeStore.deleteAttributes(subjectId);
    }

}