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

package it.smartcommunitylab.aac.attributes.mapper;

import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.DefaultAttributesImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

public abstract class BaseAttributesMapper implements AttributesMapper {

    protected final AttributeSet model;

    public BaseAttributesMapper(AttributeSet attributeSet) {
        Assert.notNull(attributeSet, "destination attribute set can not be null");
        this.model = attributeSet;
    }

    @Override
    public String getIdentifier() {
        return model.getIdentifier();
    }

    @Override
    public AttributeSet mapAttributes(Map<String, Serializable> attributesMap) {
        // create new set

        List<Attribute> attributes = new ArrayList<>();
        for (Attribute a : model.getAttributes()) {
            // TODO handle multiple
            Attribute attr = getAttribute(a, attributesMap);
            if (attr != null) {
                attributes.add(attr);
            }
        }

        DefaultAttributesImpl ua = new DefaultAttributesImpl(model.getIdentifier(), attributes);
        ua.setName(model.getName());
        ua.setDescription(model.getDescription());
        return ua;
    }

    protected abstract Attribute getAttribute(Attribute attribute, Map<String, Serializable> attributes);
}
