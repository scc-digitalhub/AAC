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

package it.smartcommunitylab.aac.accounts.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.users.base.AbstractBaseUserResource;
import java.io.Serializable;
import java.util.Map;

/*
 * Abstract class for user accounts
 *
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
// @JsonSubTypes(
//     {
//         @Type(value = InternalUserAccount.class, name = InternalUserAccount.RESOURCE_TYPE),
//         @Type(value = OIDCUserAccount.class, name = OIDCUserAccount.RESOURCE_TYPE),
//         @Type(value = SamlUserAccount.class, name = SamlUserAccount.RESOURCE_TYPE),
//     }
// )
public abstract class AbstractUserAccount extends AbstractBaseUserResource implements UserAccount {

    protected AbstractUserAccount(String authority, String provider, String realm, String id) {
        super(authority, provider, realm, id, null);
    }

    protected AbstractUserAccount(String authority, String provider, String realm, String id, String userId) {
        super(authority, provider, realm, id, userId);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    protected AbstractUserAccount() {
        super((String) null, (String) null, (String) null, (String) null, (String) null);
    }

    @Override
    public String getId() {
        return getUuid();
    }

    // uuid is mandatory
    public abstract String getUuid();

    // repositoryId is always available
    public abstract String getRepositoryId();

    // account status is manageable
    public abstract String getStatus();

    public abstract void setStatus(String status);

    // accounts store attributes as maps
    public abstract Map<String, Serializable> getAttributes();

    public abstract void setAttributes(Map<String, Serializable> attributes);
}
