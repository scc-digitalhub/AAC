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
 * A registry for claims extractors.
 *
 * We don't expect providers to be immutable:
 * the registry implementations are supposed to retrieve extractors from providers at each request
 */
public interface ExtractorsRegistry {
    /*
     * Providers
     */
    public void registerExtractorProvider(ScopeClaimsExtractorProvider provider);

    public void registerExtractorProvider(ResourceClaimsExtractorProvider provider);

    public void unregisterExtractorProvider(ScopeClaimsExtractorProvider provider);

    public void unregisterExtractorProvider(ResourceClaimsExtractorProvider provider);

    /*
     * Extractors
     */

    public Collection<ResourceClaimsExtractor> getResourceExtractors(String resourceId);

    public Collection<ScopeClaimsExtractor> getScopeExtractors(String scope);
}
