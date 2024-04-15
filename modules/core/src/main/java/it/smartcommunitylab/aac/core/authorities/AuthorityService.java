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

package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.model.Resource;
import java.util.Collection;

public interface AuthorityService<A extends ProviderAuthority<? extends ResourceProvider<? extends Resource>>> {
    /*
     * Details
     */
    public String getType();

    /*
     * Authorities read-only
     */
    public Collection<A> getAuthorities();

    public Collection<String> getAuthoritiesIds();

    public A findAuthority(String authorityId);

    public A getAuthority(String authorityId) throws NoSuchAuthorityException;
}
