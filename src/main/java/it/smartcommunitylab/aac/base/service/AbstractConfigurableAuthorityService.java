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

package it.smartcommunitylab.aac.base.service;

import it.smartcommunitylab.aac.core.authorities.ConfigurableAuthorityService;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.config.ProviderConfig;

public abstract class AbstractConfigurableAuthorityService<
    A extends ConfigurableProviderAuthority<? extends ConfigurableResourceProvider<? extends Resource, ? extends ProviderConfig<S, ? extends ConfigMap>, S, ? extends ConfigMap>, ? extends ConfigurableProvider<S>, ? extends ProviderConfig<S, ? extends ConfigMap>, S, ? extends ConfigMap>,
    S extends ConfigMap
>
    extends AbstractAuthorityService<A>
    implements ConfigurableAuthorityService<A, S> {

    protected AbstractConfigurableAuthorityService(String type) {
        super(type);
    }
}
