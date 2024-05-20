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

import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.model.Resource;

/*
 * Provider authorities handle (configurable) resource providers by managing registrations and configuration
 */
public interface ConfigurableProviderAuthority<
    T extends ConfigurableResourceProvider<? extends Resource, P, S, M>,
    P extends ProviderConfig<S, M>,
    S extends ConfigMap,
    M extends ConfigMap
>
    extends ProviderAuthority<T> {
    /*
     * Registration
     */

    public P registerProvider(P config) throws IllegalArgumentException, RegistrationException, SystemException;

    public void unregisterProvider(String providerId) throws SystemException;
    /*
     * Config provider exposes configuration
     */
    // public @Nullable ConfigurationProvider<P, C, S, M> getConfigurationProvider();
}
