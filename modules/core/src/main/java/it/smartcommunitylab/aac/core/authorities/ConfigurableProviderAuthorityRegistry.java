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

import com.fasterxml.jackson.databind.JsonNode;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.model.Resource;

public interface ConfigurableProviderAuthorityRegistry<
    A extends ConfigurableProviderAuthority<T, P, S, ? extends ConfigMap>,
    T extends ConfigurableResourceProvider<? extends Resource, P, S, ? extends ConfigMap>,
    P extends ProviderConfig<S, ? extends ConfigMap>,
    S extends ConfigMap
>
    extends ProviderAuthorityRegistry<A> {
    public JsonNode getSettingsSchema(String authority) throws NoSuchAuthorityException;

    public JsonNode getConfigurationSchema(String authority) throws NoSuchAuthorityException;
}
