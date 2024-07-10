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

package it.smartcommunitylab.aac.attributes.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.DefaultAttributesImpl;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.base.model.AbstractBaseUserResource;

/*
 * Abstract class for user attributes
 *
 * all implementations should derive from this
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", defaultImpl = DefaultAttributesImpl.class)
@JsonSubTypes({ @Type(value = DefaultAttributesImpl.class, name = SystemKeys.RESOURCE_ATTRIBUTES) })
public abstract class AbstractAttributes extends AbstractBaseUserResource implements UserAttributes {

    protected AbstractAttributes(String authority, String provider, String realm) {
        super(authority, provider, realm, null, null);
    }

    protected AbstractAttributes(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, null, userId);
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
}
