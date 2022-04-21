package it.smartcommunitylab.aac.spid.provider;

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
import it.smartcommunitylab.aac.spid.attributes.SpidAttributesMapper;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;

public class SpidAttributeProvider
        extends AbstractIdentityAttributeProvider<SpidAuthenticatedPrincipal, SpidUserAccount> {

    private final OpenIdAttributesMapper openidMapper;
    private final SpidAttributesMapper spidMapper;

    public SpidAttributeProvider(
            String providerId,
            SpidIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");

        // attributes
        openidMapper = new OpenIdAttributesMapper();
        spidMapper = new SpidAttributesMapper();

        // make sure store is disabled
        this.attributeStore = null;
    }

    @Override
    protected List<UserAttributes> extractUserAttributes(SpidUserAccount account,
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
        emailset.setEmailVerified(StringUtils.hasText(account.getEmail()));
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

        // spid via mapper
        AttributeSet spidset = spidMapper.mapAttributes(map);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                spidset));

        // openid via mapper
        AttributeSet openidset = openidMapper.mapAttributes(map);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                openidset));

        if (principalAttributes != null) {
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

}