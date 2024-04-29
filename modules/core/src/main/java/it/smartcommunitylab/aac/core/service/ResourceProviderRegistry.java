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

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.model.Resource;
import java.util.Collection;

/*
 * Resource provider service handles resource providers for consumers.
 * It abstract away the existence of authorities, modules etc
 */
public interface ResourceProviderRegistry<S extends ResourceProvider<? extends Resource>> {
    public boolean hasResourceProvider(String providerId);

    public S findResourceProvider(String providerId);

    public S getResourceProvider(String providerId) throws NoSuchProviderException;

    public Collection<S> listResourceProviders();

    public Collection<S> listResourceProvidersByRealm(String realm);
}
