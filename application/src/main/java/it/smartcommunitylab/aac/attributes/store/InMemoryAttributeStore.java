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

package it.smartcommunitylab.aac.attributes.store;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.Assert;

public class InMemoryAttributeStore implements AttributeStore {

    private final Map<String, Map<String, Serializable>> attributes;

    private final String authority;
    private final String provider;

    public InMemoryAttributeStore(String authority, String provider) {
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.hasText(provider, "provider can not be null or empty");
        this.authority = authority;
        this.provider = provider;

        this.attributes = new ConcurrentHashMap<>();
    }

    public String getAuthority() {
        return authority;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public Serializable getAttribute(String entityId, String key) {
        if (!attributes.containsKey(entityId)) {
            return null;
        }

        return attributes.get(entityId).get(key);
    }

    @Override
    public Map<String, Serializable> findAttributes(String entityId) {
        return Collections.unmodifiableMap(attributes.get(entityId));
    }

    @Override
    public void setAttributes(String entityId, Set<Entry<String, Serializable>> attributesSet) {
        Map<String, Serializable> attributesMap = new HashMap<>();
        for (Entry<String, Serializable> entry : attributesSet) {
            attributesMap.put(entry.getKey(), entry.getValue());
        }

        this.attributes.put(entityId, attributesMap);
    }

    @Override
    public void addAttribute(String entityId, String key, Serializable value) {
        if (!this.attributes.containsKey(entityId)) {
            this.attributes.put(entityId, new HashMap<>());
        }

        this.attributes.get(entityId).put(key, value);
    }

    @Override
    public void updateAttribute(String entityId, String key, Serializable value) throws NoSuchElementException {
        if (!this.attributes.containsKey(entityId)) {
            throw new NoSuchElementException();
        }

        if (this.attributes.get(entityId).get(key) == null) {
            throw new NoSuchElementException();
        }

        this.attributes.get(entityId).put(key, value);
    }

    @Override
    public void deleteAttribute(String entityId, String key) {
        if (this.attributes.containsKey(entityId)) {
            this.attributes.get(entityId).remove(key);
        }
    }

    @Override
    public void deleteAttributes(String entityId) {
        if (this.attributes.containsKey(entityId)) {
            this.attributes.remove(entityId);
        }
    }
}
