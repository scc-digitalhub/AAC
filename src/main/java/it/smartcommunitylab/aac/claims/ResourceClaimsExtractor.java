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
import it.smartcommunitylab.aac.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/*
 * Claim extractor is the interface service need to expose to be able to produce claims.
 * Note that we expect implementations to always return a valid set (or null) and not perform authorization decisions here.
 *
 * When asked, either return the claimSet or null.
 * We also need extractors to identify themselves via resourceId.
 *
 * Note that extractors will be given the whole list of scopes requested for building their response.
 *
 * This interface is separated from scopeExtractor to address resources wanting to produce claims when included as audience
 */

public interface ResourceClaimsExtractor {
    public String getRealm();

    // id of resource this extractor will answer to
    public String getResourceId();

    public ClaimsSet extractUserClaims(
        String resourceId,
        User user,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException;

    public ClaimsSet extractClientClaims(
        String resourceId,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) throws InvalidDefinitionException, SystemException;
}
