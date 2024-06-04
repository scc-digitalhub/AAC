/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.registry;

import it.smartcommunitylab.aac.config.SpidProperties;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.service.SpidRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalSpidRegistry implements SpidRegistry {

    private final Map<String, SpidRegistration> identityProviders;

    public LocalSpidRegistry() {
        identityProviders = new HashMap<>();
    }

    public LocalSpidRegistry(SpidProperties properties) {
        this(properties.getIdentityProviders());
    }

    public LocalSpidRegistry(Collection<SpidRegistration> idps) {
        Map<String, SpidRegistration> registryMap = idps
            .stream()
            .collect(Collectors.toMap(SpidRegistration::getEntityId, e -> e));
        identityProviders = Collections.unmodifiableMap(registryMap);
    }

    @Override
    public Collection<SpidRegistration> getIdentityProviders() {
        return identityProviders.values();
    }

    @Override
    public SpidRegistration getIdentityProvider(String entityId) {
        return identityProviders.get(entityId);
    }
}
