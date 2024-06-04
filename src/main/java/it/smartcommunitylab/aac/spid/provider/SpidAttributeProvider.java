/*
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.attributes.mapper.SpidAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityAttributeProvider;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SpidAttributeProvider
    extends AbstractIdentityAttributeProvider<SpidUserAuthenticatedPrincipal, SamlUserAccount> {

    private final OpenIdAttributesMapper openidMapper;
    private final SpidAttributesMapper spidMapper;

    public SpidAttributeProvider(String providerId, SpidIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");
        openidMapper = new OpenIdAttributesMapper();
        spidMapper = new SpidAttributesMapper();
    }

    @Override
    protected List<UserAttributes> extractUserAttributes(
        SamlUserAccount account,
        Map<String, Serializable> principalAttributes
    ) {
        List<UserAttributes> attributes = new ArrayList<>();
        // userid
        String userId = account.getUserId();

        // core attributes
        String email = account.getEmail();
        String name = account.getName() != null ? account.getName() : account.getSubjectId();
        @Nullable
        String surname = account.getSurname();
        String username = account.getUsername() != null ? account.getUsername() : account.getEmail();

        BasicAttributesSet basicSet = new BasicAttributesSet();
        basicSet.setEmail(email);
        basicSet.setName(name);
        basicSet.setUsername(username);
        basicSet.setSurname(surname);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, basicSet));

        AccountAttributesSet accountSet = new AccountAttributesSet();
        accountSet.setUsername(account.getUsername());
        accountSet.setUserId(account.getUserId());
        accountSet.setId(account.getSubjectId());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, accountSet));

        EmailAttributesSet emailSet = new EmailAttributesSet();
        emailSet.setEmail(email);
        emailSet.setEmailVerified(StringUtils.hasText(email)); // SPID email is always verified if present
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, emailSet));

        // merge attributes
        Map<String, Serializable> map = new HashMap<>();
        if (principalAttributes != null) {
            map.putAll(principalAttributes);
        }
        // override attributes with values from account
        map.put(OpenIdAttributesSet.NAME, name);
        map.put(OpenIdAttributesSet.EMAIL, email);
        map.put(OpenIdAttributesSet.EMAIL_VERIFIED, account.isEmailVerified());
        map.put(OpenIdAttributesSet.PREFERRED_USERNAME, username);
        if (StringUtils.hasText(surname)) {
            map.put(OpenIdAttributesSet.FAMILY_NAME, surname);
        }
        // update via mapper(s)
        AttributeSet spidSet = spidMapper.mapAttributes(map);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, spidSet));
        AttributeSet openidSet = openidMapper.mapAttributes(map);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, openidSet));

        // build an additional attributeSet for additional attributes, specific for this
        // provider, where we export all raw attributes
        if (principalAttributes != null) {
            DefaultUserAttributesImpl idpset = new DefaultUserAttributesImpl(
                getAuthority(),
                getProvider(),
                getRealm(),
                userId,
                "idp." + getProvider()
            );
            // store everything as string - if not possible throw away the attribute
            for (Map.Entry<String, Serializable> e : principalAttributes.entrySet()) {
                try {
                    idpset.addAttribute(new StringAttribute(e.getKey(), StringAttribute.parseValue(e.getValue())));
                } catch (ParseException ignored) {}
            }
            attributes.add(idpset);
        }
        return attributes;
    }
}
