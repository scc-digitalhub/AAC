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

package it.smartcommunitylab.aac.base.service;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.core.service.ResourceProviderRegistry;
import it.smartcommunitylab.aac.model.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

@Slf4j
public abstract class AbstractProviderRegistry<T extends ResourceProvider<? extends Resource>>
    implements ResourceProviderRegistry<T>, InitializingBean {

    protected final String type;
    protected final Map<String, ProviderAuthority<T>> authorities = new HashMap<>();

    protected AbstractProviderRegistry() {
        //extract type as type info
        ResolvableType resolvableType = ResolvableType.forClass(getClass());
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) resolvableType.getSuperType().getGeneric(0).resolve();
        if (clazz == null) {
            throw new IllegalArgumentException("class is not resolvable");
        }
        this.type = clazz.getSimpleName();
    }

    @Autowired
    public void setAuthorities(List<ProviderAuthority<T>> authorities) {
        Assert.notNull(authorities, "authorities are required");

        this.authorities.clear();

        authorities
            .stream()
            .forEach(a -> {
                this.authorities.put(a.getAuthorityId(), a);
            });

        log.debug("registered authorities for {}: {}", type, this.authorities.keySet());
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(authorities, "authorities can not be null");
    }

    @Override
    public boolean hasResourceProvider(String providerId) {
        // ask authority
        return authorities.values().stream().anyMatch(a -> a.hasProvider(providerId));
    }

    @Override
    public T findResourceProvider(String providerId) {
        // ask authority
        return authorities
            .values()
            .stream()
            .map(a -> a.findProvider(providerId))
            .filter(p -> p != null)
            .findFirst()
            .orElse(null);
    }

    @Override
    public T getResourceProvider(String providerId) throws NoSuchProviderException {
        // ask authority
        return authorities
            .values()
            .stream()
            .map(a -> a.findProvider(providerId))
            .filter(p -> p != null)
            .findFirst()
            .orElseThrow(() -> new NoSuchProviderException());
    }

    @Override
    public Collection<T> listResourceProviders() {
        // ask authority
        return authorities.values().stream().flatMap(a -> a.listProviders().stream()).collect(Collectors.toList());
    }

    @Override
    public Collection<T> listResourceProvidersByRealm(String realm) {
        // ask authority
        return authorities
            .values()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .collect(Collectors.toList());
    }
}
