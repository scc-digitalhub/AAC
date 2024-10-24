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

import it.smartcommunitylab.aac.attributes.base.BaseAttributeSet;
import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BasicAttributesSet extends BaseAttributeSet {

    public static final String IDENTIFIER = "aac.basic";
    private static final List<String> keys;

    private Map<String, Attribute> attributes;

    public BasicAttributesSet() {
        this.attributes = new HashMap<>();
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Collection<String> getKeys() {
        return keys;
    }

    @Override
    public Collection<Attribute> getAttributes() {
        return attributes.values();
    }

    // basic attributes
    public void setUsername(String username) {
        if (username == null) {
            attributes.remove(USERNAME);
            return;
        }

        StringAttribute attr = new StringAttribute(USERNAME);
        attr.setValue(username);

        attributes.put(USERNAME, attr);
    }

    public void setName(String name) {
        if (name == null) {
            attributes.remove(NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(NAME);
        attr.setValue(name);

        attributes.put(NAME, attr);
    }

    public void setSurname(String surname) {
        if (surname == null) {
            attributes.remove(SURNAME);
            return;
        }

        StringAttribute attr = new StringAttribute(SURNAME);
        attr.setValue(surname);

        attributes.put(SURNAME, attr);
    }

    public void setEmail(String email) {
        if (email == null) {
            attributes.remove(EMAIL);
            return;
        }

        StringAttribute attr = new StringAttribute(EMAIL);
        attr.setValue(email);

        attributes.put(EMAIL, attr);
    }

    @Override
    public String getName() {
        // TODO i18n
        return "User base attributes";
    }

    @Override
    public String getDescription() {
        return "Base information about user";
    }

    public static final String USERNAME = "username";
    public static final String NAME = "name";
    public static final String SURNAME = "surname";
    public static final String EMAIL = "email";

    static {
        List<String> k = new ArrayList<>();
        k.add(USERNAME);
        k.add(NAME);
        k.add(SURNAME);
        k.add(EMAIL);
        keys = Collections.unmodifiableList(k);
    }
}
