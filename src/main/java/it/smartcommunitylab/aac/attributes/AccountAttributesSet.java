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

import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AccountAttributesSet implements AttributeSet {

    public static final String IDENTIFIER = "aac.account";
    private static final List<String> keys;

    private Map<String, Attribute> attributes;

    public AccountAttributesSet() {
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

    public void setUserId(String userId) {
        if (userId == null) {
            attributes.remove(USER_ID);
            return;
        }

        StringAttribute attr = new StringAttribute(USER_ID);
        attr.setValue(userId);

        attributes.put(USER_ID, attr);
    }

    public void setId(String id) {
        if (id == null) {
            attributes.remove(ID);
            return;
        }

        StringAttribute attr = new StringAttribute(ID);
        attr.setValue(id);

        attributes.put(ID, attr);
    }

    // additional attributes
    public void setAttribute(String key, String value) {
        if (key == null) {
            return;
        }

        if (USERNAME.equals(key) || USER_ID.equals(key) || ID.equals(key)) {
            return;
        }

        if (value == null) {
            attributes.remove(key);
            return;
        }

        StringAttribute attr = new StringAttribute(key);
        attr.setValue(value);
        attributes.put(key, attr);
    }

    @Override
    public String getName() {
        // TODO i18n
        return "User account attributes";
    }

    @Override
    public String getDescription() {
        return "Details about user accounts";
    }

    public static final String USER_ID = "user_id";
    public static final String USERNAME = "username";
    public static final String ID = "id";

    static {
        List<String> k = new ArrayList<>();
        k.add(USERNAME);
        k.add(USER_ID);
        k.add(ID);
        keys = Collections.unmodifiableList(k);
    }
}
