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

package it.smartcommunitylab.aac.credentials;

import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.credentials.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.credentials.model.EditableUserCredentials;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.credentials.provider.CredentialsService;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceConfig;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import it.smartcommunitylab.aac.model.ConfigMap;

public interface CredentialsServiceAuthority<
    S extends CredentialsService<R, E, M, C>,
    R extends UserCredentials,
    E extends EditableUserCredentials,
    M extends ConfigMap,
    C extends CredentialsServiceConfig<M>
>
    extends ConfigurableProviderAuthority<S, ConfigurableCredentialsProvider, C, CredentialsServiceSettingsMap, M> {}
