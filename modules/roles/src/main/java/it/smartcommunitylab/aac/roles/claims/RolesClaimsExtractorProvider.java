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

package it.smartcommunitylab.aac.roles.claims;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.roles.scopes.UserRolesScope;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RolesClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private static final Map<String, ScopeClaimsExtractor> extractors;

    static {
        Map<String, ScopeClaimsExtractor> e = new HashMap<>();
        e.put(UserRolesScope.SCOPE, new UserRolesClaimsExtractor());
        e.put(Config.SCOPE_CLIENT_ROLE, new ClientRolesClaimsExtractor());

        extractors = e;
    }

    @Override
    public String getResourceId() {
        return "aac.roles";
    }

    @Override
    public Collection<String> getScopes() {
        return extractors.keySet();
    }

    //    @Override
    //    public Collection<ScopeClaimsExtractor> getExtractors() {
    //        return Collections.singleton(extractor);
    //    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        ScopeClaimsExtractor extractor = extractors.get(scope);
        if (extractor == null) {
            throw new IllegalArgumentException("invalid scope");
        }

        return extractor;
    }
}
