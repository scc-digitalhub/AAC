package it.smartcommunitylab.aac.saml.provider;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAttributeProvider;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

public class SamlAttributeProvider
        extends AbstractIdentityAttributeProvider<SamlUserAuthenticatedPrincipal, SamlUserAccount> {

    private final OpenIdAttributesMapper openidMapper;

    public SamlAttributeProvider(
            String providerId,
            SamlIdentityProviderConfig providerConfig,
            String realm) {
        this(SystemKeys.AUTHORITY_SAML, providerId, providerConfig, realm);
    }

    public SamlAttributeProvider(
            String authority, String providerId,
            SamlIdentityProviderConfig providerConfig,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");

        // attributes mapping
        openidMapper = new OpenIdAttributesMapper();
    }

    @Override
    protected List<UserAttributes> extractUserAttributes(SamlUserAccount account,
            Map<String, Serializable> principalAttributes) {
        List<UserAttributes> attributes = new ArrayList<>();
        // user identifier
        String userId = account.getUserId();

        // base attributes
        String name = account.getName() != null ? account.getName() : account.getSubjectId();
        String surname = account.getSurname();
        String email = account.getEmail();
        String username = account.getUsername() != null ? account.getUsername() : account.getEmail();

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        basicset.setName(name);
        basicset.setSurname(surname);
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
        map.put(OpenIdAttributesSet.FAMILY_NAME, surname);
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

}