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
import java.util.Collection;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Component
public class SpacesClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private final SpacesClaimsExtractor extractor;

    public SpacesClaimsExtractorProvider() {
        this.extractor = new SpacesClaimsExtractor();
    }

    @Override
    public String getResourceId() {
        return "aac.roles";
    }

    @Override
    public Collection<String> getScopes() {
        return Collections.singleton(Config.SCOPE_USER_SPACES);
    }

    //    @Override
    //    public Collection<ScopeClaimsExtractor> getExtractors() {
    //        return Collections.singleton(extractor);
    //    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        if (!Config.SCOPE_USER_SPACES.equals(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        return extractor;
    }
}
