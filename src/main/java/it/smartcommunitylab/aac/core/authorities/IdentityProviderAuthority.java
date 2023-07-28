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

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.config.IdentityProviderConfig;

public interface IdentityProviderAuthority<
    S extends IdentityProvider<I, ?, ?, M, C>,
    I extends UserIdentity,
    M extends ConfigMap,
    C extends IdentityProviderConfig<M>
>
    extends ConfigurableProviderAuthority<S, I, ConfigurableIdentityProvider, M, C> {
    /*
     * Filter provider exposes auth filters for registration in filter chain
     */
    public FilterProvider getFilterProvider();
    //    /*
    //     * Config provider exposes configuration validation and schema
    //     */
    //    public IdentityConfigurationProvider<C, P> getConfigurationProvider();

}
