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

package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

public class InMemoryProviderConfigRepository<U extends AbstractProviderConfig<?, ?>>
    implements ProviderConfigRepository<U> {

    private final Map<String, U> registrations;

    public InMemoryProviderConfigRepository() {
        this.registrations = new ConcurrentHashMap<>();
    }

    @Override
    public U findByProviderId(String providerId) {
        Assert.hasText(providerId, "providerId cannot be empty");
        return registrations.get(providerId);
    }

    @Override
    public void addRegistration(U registration) {
        registrations.put(registration.getProvider(), registration);
    }

    @Override
    public void removeRegistration(String providerId) {
        registrations.remove(providerId);
    }

    @Override
    public void removeRegistration(U registration) {
        registrations.remove(registration.getProvider());
    }

    @Override
    public Collection<U> findAll() {
        return registrations.values();
    }

    @Override
    public Collection<U> findByRealm(String realm) {
        return registrations.values().stream().filter(p -> p.getRealm().equals(realm)).collect(Collectors.toList());
    }
}
