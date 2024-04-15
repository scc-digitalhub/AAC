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

import java.util.Collection;

/*
 * An attribute set defining properties related to entities.
 * When used in protected scenarios, access to set content will be filtered based on scopes and authorizations
 */
public interface AttributeSet {
    /*
     * The set identifier should match a scope, which when approved will enable
     * access to this set
     */
    public String getIdentifier();

    /*
     * The set keys, as per definition *not content*
     */
    public Collection<String> getKeys();

    /*
     * The attribute list (content)
     */
    public Collection<Attribute> getAttributes();

    /*
     * Human readable
     */
    public String getName();

    public String getDescription();
}
