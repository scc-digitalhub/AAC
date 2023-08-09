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

package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

/*
 * Default Attributes describes an attributeSet as a model definition, detached from user resources.
 * Keys are required and should present the whole list of attributes available from the set.
 * The collection of attributes can contain descriptive or sample values.
 *
 * Providers are not required to fulfill all keys, consumers should be able to handle null and react accordingly
 *
 * Models do not persist a relation with a provider, but provider-specific collections should be namespaced.
 */
public class DefaultAttributesImpl implements AttributeSet, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String identifier;

    private String name;
    private String description;

    private Set<String> keys;

    private Set<Attribute> attributes;

    private boolean isMutable;

    public DefaultAttributesImpl(String identifier) {
        Assert.hasText(identifier, "a valid identifier is required");
        this.identifier = identifier;
        this.keys = new HashSet<>();
        this.attributes = new HashSet<>();
        this.isMutable = true;
    }

    public DefaultAttributesImpl(String identifier, String[] keys) {
        Assert.hasText(identifier, "a valid identifier is required");
        this.identifier = identifier;
        this.keys = new HashSet<>();
        this.keys.addAll(Arrays.asList(keys));
        this.attributes = new HashSet<>();
        this.isMutable = true;
    }

    public DefaultAttributesImpl(String identifier, Collection<Attribute> attributes) {
        Assert.hasText(identifier, "a valid identifier is required");
        this.identifier = identifier;
        this.keys = new HashSet<>();
        this.attributes = new HashSet<>();
        this.isMutable = true;
        this.addAttributes(attributes);
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<String> getKeys() {
        // we protect our representation from modifications
        return Collections.unmodifiableCollection(keys);
    }

    public void setKeys(Collection<String> keys) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.keys = new HashSet<>();
        this.keys.addAll(keys);
    }

    public Set<Attribute> getAttributes() {
        // we protect our representation from modifications
        return Collections.unmodifiableSet(attributes);
    }

    public void setAttributes(Collection<Attribute> attributes) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.attributes = new HashSet<>();
        this.attributes.addAll(attributes);
        this.keys = new HashSet<>();
        this.keys.addAll(attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet()));
    }

    public void addAttributes(Collection<Attribute> attributes) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.attributes.addAll(attributes);
        this.keys.addAll(attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet()));
    }

    public void addAttribute(Attribute attr) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.attributes.add(attr);
        this.keys.add(attr.getKey());
    }
}
