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
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Collection;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.springframework.web.bind.MethodArgumentNotValidException;

/*
 * Persistence for providers, in the form of persisted configurable models
 */
public interface ConfigurableProviderService<C extends ConfigurableProvider<? extends ConfigMap>> {
    /*
     * Configuration
     */
    Collection<C> listConfigurableProviders();
    Collection<C> listConfigurableProvidersByRealm(@NotNull String realm);

    C findConfigurableProvider(@NotNull String providerId);
    C getConfigurableProvider(@NotNull String providerId) throws NoSuchProviderException;

    C addConfigurableProvider(@NotNull String realm, @Nullable String providerId, @NotNull C cp)
        throws RegistrationException, SystemException, NoSuchAuthorityException, MethodArgumentNotValidException;
    C updateConfigurableProvider(@NotNull String providerId, @NotNull C cp)
        throws NoSuchProviderException, NoSuchAuthorityException, RegistrationException, MethodArgumentNotValidException;
    void deleteConfigurableProvider(@NotNull String providerId) throws SystemException, NoSuchProviderException;
    // /*
    //  * Registration
    //  * TODO move to dedicated service
    //  */
    // void registerProvider(String providerId)
    //     throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException;
    // void unregisterProvider(String providerId)
    //     throws NoSuchProviderException, SystemException, NoSuchAuthorityException;
    // boolean isProviderRegistered(String providerId) throws NoSuchProviderException, NoSuchAuthorityException;
}
