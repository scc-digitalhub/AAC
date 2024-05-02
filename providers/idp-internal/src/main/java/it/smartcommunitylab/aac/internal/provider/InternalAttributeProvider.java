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

package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.model.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityAttributeProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InternalAttributeProvider<P extends InternalUserAuthenticatedPrincipal>
    extends AbstractIdentityAttributeProvider<P, InternalUserAccount> {

    public InternalAttributeProvider(String providerId, String realm) {
        this(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
    }

    public InternalAttributeProvider(String authority, String providerId, String realm) {
        super(authority, providerId, realm);
    }

    @Override
    protected List<UserAttributes> extractUserAttributes(
        InternalUserAccount account,
        Map<String, Serializable> principalAttributes
    ) {
        List<UserAttributes> attributes = new ArrayList<>();
        String userId = account.getUserId();

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        basicset.setName(account.getName());
        basicset.setSurname(account.getSurname());
        basicset.setEmail(account.getEmail());
        basicset.setUsername(account.getUsername());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, basicset));

        // account
        AccountAttributesSet accountset = new AccountAttributesSet();
        accountset.setUsername(account.getUsername());
        accountset.setUserId(account.getUserId());
        accountset.setId(account.getUsername());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, accountset));

        // email
        EmailAttributesSet emailset = new EmailAttributesSet();
        emailset.setEmail(account.getEmail());
        emailset.setEmailVerified(account.isConfirmed());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, emailset));

        // openid fixed
        OpenIdAttributesSet openidset = new OpenIdAttributesSet();
        openidset.setPreferredUsername(account.getUsername());
        openidset.setName(account.getName());
        openidset.setGivenName(account.getName());
        openidset.setFamilyName(account.getSurname());
        openidset.setEmail(account.getEmail());
        openidset.setEmailVerified(account.isConfirmed());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId, openidset));

        return attributes;
    }
}
