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
import it.smartcommunitylab.aac.attributes.base.AbstractUserAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

/*
 * Default User Attributes is an instantiable bean which contains attributes bound to a user
 */

public class DefaultUserAttributesImpl extends AbstractUserAttributes {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String identifier;

    private Set<Attribute> attributes;

    private String uuid;
    private String name;
    private String description;

    public DefaultUserAttributesImpl(
        String authority,
        String provider,
        String realm,
        String userId,
        String identifier
    ) {
        super(authority, provider, realm, userId);
        Assert.hasText(identifier, "set identifier can not be null");

        this.identifier = identifier;
        this.attributes = new HashSet<>();
    }

    public DefaultUserAttributesImpl(
        String authority,
        String provider,
        String realm,
        String userId,
        AttributeSet attributeSet
    ) {
        super(authority, provider, realm, userId);
        Assert.notNull(attributeSet, "attribute set can not be null");
        Assert.hasText(attributeSet.getIdentifier(), "set identifier can not be null");

        this.identifier = attributeSet.getIdentifier();
        this.attributes = new HashSet<>();
        this.attributes.addAll(attributeSet.getAttributes());
        this.name = attributeSet.getName();
        this.description = attributeSet.getDescription();
    }

    public String getAttributesId() {
        return getIdentifier();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public Collection<Attribute> getAttributes() {
        return attributes;
    }

    public void addAttributes(Collection<Attribute> attributes) {
        this.attributes.addAll(attributes);
    }

    public void addAttribute(Attribute attr) {
        this.attributes.add(attr);
    }

    public void deleteAttribute(String key) {
        Set<Attribute> toRemove = attributes.stream().filter(a -> a.getKey().equals(key)).collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            attributes.removeAll(toRemove);
        }
    }

    public boolean hasAttribute(String key) {
        Set<Attribute> match = attributes.stream().filter(a -> a.getKey().equals(key)).collect(Collectors.toSet());
        return !match.isEmpty();
    }

    public Collection<Attribute> getAttribute(String key) {
        return attributes.stream().filter(a -> a.getKey().equals(key)).collect(Collectors.toSet());
    }

    @Override
    public Collection<String> getKeys() {
        return attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet());
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
