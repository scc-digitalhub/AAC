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

package it.smartcommunitylab.aac.profiles.claims;

import it.smartcommunitylab.aac.claims.ScopeClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class AccountProfileClaimsExtractorProvider implements ScopeClaimsExtractorProvider {

    private final AccountProfileClaimsExtractor extractor;

    public AccountProfileClaimsExtractorProvider() {
        this.extractor = new AccountProfileClaimsExtractor();
    }

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".account";
    }

    @Override
    public Collection<String> getScopes() {
        return extractor.getScopes();
    }

    //    @Override
    //    public Collection<ScopeClaimsExtractor> getExtractors() {
    //        return Collections.singleton(extractor);
    //    }

    @Override
    public ScopeClaimsExtractor getExtractor(String scope) {
        if (extractor.getScopes().contains(scope)) {
            return extractor;
        }

        throw new IllegalArgumentException("invalid scope");
    }
}
