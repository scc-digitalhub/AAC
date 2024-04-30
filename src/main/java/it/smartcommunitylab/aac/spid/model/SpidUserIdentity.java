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

package it.smartcommunitylab.aac.spid.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.identity.base.AbstractUserIdentity;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.Assert;

// TODO: review whether this class is necessary, as current implementation are OIDCUserIndentity, InternalUserIdentity and
public class SpidUserIdentity extends AbstractUserIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_IDENTITY + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_SAML;
    private SpidUserAuthenticatedPrincipal principal;

    private final SamlUserAccount account;
    private Set<UserAttributes> attributes;

    public SpidUserIdentity(
        String authority,
        String provider,
        String realm,
        SamlUserAccount account,
        SpidUserAuthenticatedPrincipal principal
    ) {
        super(authority, provider, realm, account.getUuid(), account.getUserId());
        Assert.notNull(account, "account can not be null");

        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptySet();
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public SpidUserAuthenticatedPrincipal getPrincipal() {
        return this.principal;
    }

    @Override
    public SamlUserAccount getAccount() {
        return account;
    }

    @Override
    public Collection<UserAttributes> getAttributes() {
        return attributes;
    }

    public String getSubjectId() {
        return account.getSubjectId();
    }

    public String getEmailAddress() {
        return account.getEmail();
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = new HashSet<>();
        if (attributes != null) {
            this.attributes.addAll(attributes);
        }
    }
}
