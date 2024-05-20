/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.model.Resource;
import javax.validation.constraints.NotNull;

/*
 * Configurable resource providers are backed by a config composed of shared settings map and a specific config map.
 * This service enables consumers to register/unregister dynamically providers, given a config.
 * It also handles the configuration providers responsible for translating configurable into provider configs.
 *
 */
public interface ConfigurableResourceProviderRegistry<
    S extends ConfigurableResourceProvider<? extends Resource, ? extends ProviderConfig<M, ?>, M, ? extends ConfigMap>,
    C extends ConfigurableProvider<M>,
    M extends ConfigMap
>
    extends ResourceProviderRegistry<S> {
    /*
     * Registration
     */
    void registerProvider(@NotNull String providerId, @NotNull C config)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException;
    void unregisterProvider(@NotNull String providerId)
        throws NoSuchProviderException, SystemException, NoSuchAuthorityException;
    boolean isProviderRegistered(String providerId) throws NoSuchProviderException, NoSuchAuthorityException;
}
