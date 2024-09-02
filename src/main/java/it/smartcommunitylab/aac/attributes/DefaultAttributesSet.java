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

package it.smartcommunitylab.aac.attributes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.DefaultAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultAttributesSet implements AttributeSet {

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String identifier;

    private String realm;

    private String name;
    private String description;

    private Collection<DefaultAttribute> attributes = Collections.emptyList();

    public DefaultAttributesSet() {
        this.attributes = new ArrayList<>();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    @Override
    public Collection<String> getKeys() {
        return attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet());
    }

    @Override
    public Collection<Attribute> getAttributes() {
        return Collections.unmodifiableCollection(attributes);
    }

    public void setAttributes(Collection<DefaultAttribute> attributes) {
        this.attributes = attributes;
    }
    
    public String getId() {
    	return this.identifier;
    }

    public void addAttributes(Collection<Attribute> attributes) {
        this.attributes = new HashSet<>();
        if (attributes != null) {
            this.attributes.addAll(
                    attributes
                        .stream()
                        .map(a -> {
                            DefaultAttribute attr = new DefaultAttribute();
                            attr.setKey(a.getKey());
                            attr.setType(a.getType());
                            attr.setName(a.getName());
                            attr.setDescription(a.getDescription());
                            attr.setIsMultiple(a.getIsMultiple());
                            return attr;
                        })
                        .collect(Collectors.toSet())
                );
        }
    }

    public void addAttribute(Attribute attribute) {
        if (attribute != null) {
            // translate to std attr dropping value
            DefaultAttribute attr = new DefaultAttribute();
            attr.setKey(attribute.getKey());
            attr.setType(attribute.getType());
            attr.setName(attribute.getName());
            attr.setDescription(attribute.getDescription());
            attr.setIsMultiple(attribute.getIsMultiple());
            attributes.add(attr);
        }
    }

    public static DefaultAttributesSet from(AttributeSet set) {
        DefaultAttributesSet aset = new DefaultAttributesSet();
        aset.identifier = set.getIdentifier();
        aset.realm = null;
        aset.name = set.getName();
        aset.description = set.getDescription();
        if (set.getAttributes() != null) {
            aset.attributes =
                set
                    .getAttributes()
                    .stream()
                    .map(a -> {
                        DefaultAttribute attr = new DefaultAttribute();
                        attr.setKey(a.getKey());
                        attr.setType(a.getType());
                        attr.setName(a.getName());
                        attr.setDescription(a.getDescription());
                        attr.setIsMultiple(a.getIsMultiple());
                        return attr;
                    })
                    .collect(Collectors.toSet());
        }

        return aset;
    }

    @Override
    public String toString() {
        return (
            "DefaultAttributesSet [identifier=" +
            identifier +
            ", realm=" +
            realm +
            ", name=" +
            name +
            ", description=" +
            description +
            ", attributes=" +
            attributes +
            "]"
        );
    }
}
