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

package it.smartcommunitylab.aac.claims;

import java.util.Collection;

/*
 * A claims set describing an entity
 */
public interface ClaimsSet {
    // a claimset is produced by a resource
    public String getResourceId();

    // a claim set is generated in response to a scope
    // can be null or empty
    public String getScope();

    // the set can describe the client or the user, or none
    public boolean isUser();

    public boolean isClient();

    // a claim set can be namespaced. When empty claims will be merged top level
    public String getNamespace();

    // the claim set.
    // each claim should be translated to a single value.
    // Multiple claims under the same key will be merged into a collection
    public Collection<Claim> getClaims();
}
