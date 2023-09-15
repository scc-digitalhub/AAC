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

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.users.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/*
 * Claim extractor is the interface service need to expose to be able to produce claims in response to scopes.
 * Note that we expect implementations to always return a valid set (or null) and not perform authorization decisions here.
 *
 * When asked for a scope, either return the claimSet or null.
 * We also need extractors to identify themselves via a combo (resourceid + scope).
 *
 * Note that extractors will be given the whole list of scopes requested for building their response,
 * but they need to respond to the defined scope they are invoked on.
 */

public interface ScopeClaimsExtractor {
    public String getRealm();

    public String getResourceId();

    // a list of scopes (for the declared resource) this extractor will answer to
    public Collection<String> getScopes();

    public ClaimsSet extractUserClaims(
        String scope,
        User user,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException;

    public ClaimsSet extractClientClaims(
        String scope,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException;
}
