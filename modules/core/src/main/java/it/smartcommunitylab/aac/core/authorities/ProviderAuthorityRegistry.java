/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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

public interface ProviderAuthorityRegistry<
    A extends ProviderAuthority<? extends ResourceProvider<? extends Resource>>
> {
    public A findProviderAuthority(String authorityId);

    public A getProviderAuthority(String authorityId) throws NoSuchAuthorityException;

    public Collection<A> listProviderAuthorities();
}
