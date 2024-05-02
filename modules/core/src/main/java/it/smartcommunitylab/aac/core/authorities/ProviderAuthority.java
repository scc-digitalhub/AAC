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

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.model.Resource;
import java.util.List;

/*
 * Provider authorities handle resource providers
 */
public interface ProviderAuthority<T extends ResourceProvider<? extends Resource>> {
    /*
     * Details
     */

    // public String getId();

    public String getAuthorityId();

    /*
     * Providers
     */

    public boolean hasProvider(String providerId);

    public T findProvider(String providerId);

    public T getProvider(String providerId) throws NoSuchProviderException;

    public List<T> listProviders();

    // authorities are global, let consumers filter by realm
    public List<T> getProvidersByRealm(String realm);
}
