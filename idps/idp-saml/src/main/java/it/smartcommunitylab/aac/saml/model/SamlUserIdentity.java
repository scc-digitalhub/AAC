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

package it.smartcommunitylab.aac.saml.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.identity.base.AbstractUserIdentity;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.Assert;

public class SamlUserIdentity extends AbstractUserIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_IDENTITY + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_SAML;

    // authentication principal (if available)
    private SamlUserAuthenticatedPrincipal principal;

    // user account
    private final SamlUserAccount account;

    // attribute sets
    private Set<UserAttributes> attributes;

    public SamlUserIdentity(String authority, String provider, String realm, SamlUserAccount account) {
        super(authority, provider, realm, account.getUuid(), account.getUserId());
        Assert.notNull(account, "account can not be null");

        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptySet();
    }

    public SamlUserIdentity(
        String authority,
        String provider,
        String realm,
        SamlUserAccount account,
        SamlUserAuthenticatedPrincipal principal
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
    public SamlUserAuthenticatedPrincipal getPrincipal() {
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
