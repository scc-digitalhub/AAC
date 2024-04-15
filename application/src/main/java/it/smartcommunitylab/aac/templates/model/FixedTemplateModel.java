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

package it.smartcommunitylab.aac.templates.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

public class FixedTemplateModel extends TemplateModel {

    protected final Set<String> keys;

    public FixedTemplateModel(
        String authority,
        String realm,
        String provider,
        String template,
        Collection<String> keys
    ) {
        super(authority, realm, provider, template);
        this.keys = Collections.unmodifiableSortedSet(new TreeSet<>(keys));
    }

    @Override
    public Collection<String> keys() {
        return keys;
    }

    @Override
    public String get(String key) {
        Assert.hasText(key, "key can not be null");
        if (!keys.contains(key)) {
            return null;
        }

        return super.get(key);
    }

    @Override
    public void set(String key, String value) {
        Assert.hasText(key, "key can not be null");
        if (keys.contains(key)) {
            super.set(key, value);
        }
    }

    @Override
    public void setContent(Map<String, String> content) {
        if (content != null) {
            Map<String, String> map = content
                .entrySet()
                .stream()
                .filter(e -> keys.contains(e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            super.setContent(map);
        }
    }
}
