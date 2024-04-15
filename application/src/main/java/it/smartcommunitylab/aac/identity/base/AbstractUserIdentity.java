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

package it.smartcommunitylab.aac.identity.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.base.model.AbstractBaseUserResource;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.oidc.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.saml.model.SamlUserIdentity;

/*
 * Abstract identity
 *
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    {
        @Type(value = InternalUserIdentity.class, name = InternalUserIdentity.RESOURCE_TYPE),
        @Type(value = OIDCUserIdentity.class, name = OIDCUserIdentity.RESOURCE_TYPE),
        @Type(value = SamlUserIdentity.class, name = SamlUserIdentity.RESOURCE_TYPE),
    }
)
public abstract class AbstractUserIdentity extends AbstractBaseUserResource implements UserIdentity {

    protected AbstractUserIdentity(String authority, String provider, String realm, String id) {
        super(authority, provider, realm, id, null);
    }

    protected AbstractUserIdentity(String authority, String provider, String realm, String id, String userId) {
        super(authority, provider, realm, id, userId);
    }

    // uuid is mandatory
    public String getUuid() {
        return getAccount() != null ? getAccount().getUuid() : null;
    }

    public abstract AbstractUserAccount getAccount();
}
