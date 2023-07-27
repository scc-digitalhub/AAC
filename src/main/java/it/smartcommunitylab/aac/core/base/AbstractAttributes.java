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

package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAttributes;

/*
 * Abstract class for user attributes
 *
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = DefaultAttributesImpl.class, name = SystemKeys.RESOURCE_ATTRIBUTES) })
public abstract class AbstractAttributes extends AbstractBaseUserResource implements UserAttributes {

    private String userId;
    private String realm;

    protected AbstractAttributes(String authority, String provider) {
        super(authority, provider);
    }

    // local attributes identifier, for this set for this user
    @Override
    public String getId() {
        if (getIdentifier() == null || getUserId() == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getUserId()).append(SystemKeys.URN_SEPARATOR);
        sb.append(getIdentifier());

        return sb.toString();
    }

    @Override
    public String getResourceId() {
        return getAttributesId();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
