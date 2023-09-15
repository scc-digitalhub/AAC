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

package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.users.model.UserResource;

/*
 * A set of attributes for a given user, from an authority via a provider
 *
 * Access to content should be protected via a scope "user.<setId>.me"
 * The scope protects access to content and is not directly related to claims.
 *
 * When used to build "profiles" we expected either a 1-to-1 or a many-to-1
 * relationship between attributeSets and a given profile.
 * Scopes binded to profiles should require implicit/explicit approval of set scope
 *
 */
public interface UserAttributes extends AttributeSet, UserResource {
    // // a local unique identifier for this set for this user
    // public String getAttributesId();

    // mapper to attributeSet
    // TODO?

    default String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    default String getAttributesId() {
        return getId();
    }
}
