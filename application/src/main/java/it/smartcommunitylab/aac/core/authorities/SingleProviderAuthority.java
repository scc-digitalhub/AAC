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
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.config.ProviderConfig;

/*
 * Single provider per realm
 */

public interface SingleProviderAuthority<
    P extends ConfigurableResourceProvider<? extends Resource, C, S, M>,
    C extends ProviderConfig<S, M>,
    S extends ConfigMap,
    M extends ConfigMap
> {
    public P findProviderByRealm(String realm);

    public P getProviderByRealm(String realm) throws NoSuchProviderException;
}
