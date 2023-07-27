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

package it.smartcommunitylab.aac.groups.claims;

import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.groups.scopes.ClientGroupsScope;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource;
import it.smartcommunitylab.aac.groups.scopes.UserGroupsScope;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GroupsClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private static final Map<String, ScopeClaimsExtractor> extractors;

    static {
        Map<String, ScopeClaimsExtractor> e = new HashMap<>();
        e.put(UserGroupsScope.SCOPE, new UserGroupsClaimsExtractor());
        e.put(ClientGroupsScope.SCOPE, new ClientGroupsClaimsExtractor());

        extractors = e;
    }

    @Override
    public String getResourceId() {
        return GroupsResource.RESOURCE_ID;
    }

    @Override
    public Collection<String> getScopes() {
        return extractors.keySet();
    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        ScopeClaimsExtractor extractor = extractors.get(scope);
        if (extractor == null) {
            throw new IllegalArgumentException("invalid scope");
        }

        return extractor;
    }
}
