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
import it.smartcommunitylab.aac.attributes.types.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.types.StringAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmailAttributesSet implements AttributeSet {

    public static final String IDENTIFIER = "aac.email";
    private static final List<String> keys;

    private Map<String, Attribute> attributes;

    public EmailAttributesSet() {
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

    public void setEmail(String email) {
        if (email == null) {
            attributes.remove(EMAIL);
            return;
        }

        StringAttribute attr = new StringAttribute(EMAIL);
        attr.setValue(email);

        attributes.put(EMAIL, attr);
    }

    public void setEmailVerified(Boolean emailVerified) {
        if (emailVerified == null) {
            attributes.remove(EMAIL_VERIFIED);
            return;
        }

        BooleanAttribute attr = new BooleanAttribute(EMAIL_VERIFIED);
        attr.setValue(emailVerified);

        attributes.put(EMAIL_VERIFIED, attr);
    }

    @Override
    public String getName() {
        // TODO i18n
        return "User email attributes";
    }

    @Override
    public String getDescription() {
        return "User email addresses and status";
    }

    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email_verified";

    static {
        List<String> k = new ArrayList<>();
        k.add(EMAIL);
        k.add(EMAIL_VERIFIED);
        keys = Collections.unmodifiableList(k);
    }
}
