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

package it.smartcommunitylab.aac.core.store;

import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import org.springframework.util.Assert;

/*
 * A provider config repository which creates missing configs on-the-fly via factories
 */
public class AutoProviderConfigRepository<C extends ProviderConfig<?, ?>> implements ProviderConfigRepository<C> {

    private final ProviderConfigRepository<C> repository;

    // creator builds a new config from providerId
    private Function<String, C> creator = providerId -> (null);

    // factory builds a new config from realm
    private Function<String, C> factory = realm -> (null);

    public AutoProviderConfigRepository(ProviderConfigRepository<C> baseRepository) {
        Assert.notNull(baseRepository, "base repository can not be null");

        this.repository = baseRepository;
    }

    public void setCreator(Function<String, C> creator) {
        this.creator = creator;
    }

    public void setFactory(Function<String, C> factory) {
        this.factory = factory;
    }

    @Override
    public C findByProviderId(String providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException();
        }

        C c = repository.findByProviderId(providerId);
        if (c == null) {
            // use creator and store if successful
            c = creator.apply(providerId);
            if (c != null) {
                repository.addRegistration(c);
            }
        }

        return c;
    }

    @Override
    public Collection<C> findAll() {
        return repository.findAll();
    }

    @Override
    public Collection<C> findByRealm(String realm) {
        if (realm == null) {
            throw new IllegalArgumentException();
        }

        Collection<C> list = repository.findByRealm(realm);
        if (list.isEmpty()) {
            // use factory and store if successful
            C c = factory.apply(realm);
            if (c != null) {
                repository.addRegistration(c);

                return Collections.singleton(c);
            }
        }

        return list;
    }

    @Override
    public void addRegistration(C registration) {
        repository.addRegistration(registration);
    }

    @Override
    public void removeRegistration(String providerId) {
        repository.removeRegistration(providerId);
    }

    @Override
    public void removeRegistration(C registration) {
        repository.removeRegistration(registration);
    }
}
