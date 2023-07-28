/*
 * Copyright 2023 the original author or authors
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

package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.base.provider.AbstractIdentityAttributeProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public class OIDCAttributeProvider
    extends AbstractIdentityAttributeProvider<OIDCUserAuthenticatedPrincipal, OIDCUserAccount> {

    private final OpenIdAttributesMapper openidMapper;

    public OIDCAttributeProvider(String providerId, String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, realm);
    }

    public OIDCAttributeProvider(String authority, String providerId, String realm) {
        super(authority, providerId, realm);
        // attributes mapping
        openidMapper = new OpenIdAttributesMapper();
    }

    @Override
    protected List<UserAttributes> extractUserAttributes(
        OIDCUserAccount account,
        Map<String, Serializable> principalAttributes
    ) {
        List<UserAttributes> attributes = new ArrayList<>();
        // user identifier
        String userId = account.getUserId();

        // base attributes
        String name = account.getName() != null ? account.getName() : account.getGivenName();
        String email = account.getEmail();
        String username = account.getUsername() != null ? account.getUsername() : account.getEmail();

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        basicset.setName(name);
        basicset.setSurname(account.getFamilyName());
        basicset.setEmail(email);
        basicset.setUsername(username);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, basicset));

        // account
        AccountAttributesSet accountset = new AccountAttributesSet();
        accountset.setUsername(username);
        accountset.setUserId(account.getUserId());
        accountset.setId(account.getSubject());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, accountset));

        // email
        EmailAttributesSet emailset = new EmailAttributesSet();
        emailset.setEmail(account.getEmail());
        emailset.setEmailVerified(account.getEmailVerified());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, emailset));

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

        if (StringUtils.hasText(account.getGivenName())) {
            map.put(OpenIdAttributesSet.GIVEN_NAME, account.getGivenName());
        }
        if (StringUtils.hasText(account.getFamilyName())) {
            map.put(OpenIdAttributesSet.FAMILY_NAME, account.getFamilyName());
        }
        if (StringUtils.hasText(account.getPicture())) {
            map.put(OpenIdAttributesSet.PICTURE, account.getPicture());
        }
        if (StringUtils.hasText(account.getLang())) {
            map.put(OpenIdAttributesSet.LOCALE, account.getLang());
        }

        // openid via mapper
        AttributeSet openidset = openidMapper.mapAttributes(map);
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, openidset));

        if (principalAttributes != null) {
            // build an additional attributeSet for additional attributes, specific for this
            // provider, where we export all raw attributes
            DefaultUserAttributesImpl idpset = new DefaultUserAttributesImpl(
                getAuthority(),
                getProvider(),
                getRealm(),
                userId,
                "idp." + getProvider()
            );
            // store everything as string
            for (Map.Entry<String, Serializable> e : principalAttributes.entrySet()) {
                try {
                    idpset.addAttribute(new StringAttribute(e.getKey(), StringAttribute.parseValue(e.getValue())));
                } catch (ParseException e1) {}
            }
            attributes.add(idpset);
        }

        return attributes;
    }
}
