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

package it.smartcommunitylab.aac.attributes.types;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttribute;
import it.smartcommunitylab.aac.attributes.model.AttributeType;
import java.io.Serializable;
import java.text.ParseException;

public class BooleanAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private Boolean value;

    public BooleanAttribute(String key) {
        this.key = key;
    }

    public BooleanAttribute(String key, Boolean boo) {
        this.key = key;
        this.value = boo;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.BOOLEAN;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    public static Boolean parseValue(Serializable value) throws ParseException {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        String stringValue = String.valueOf(value);
        // check if value is 1/0 in addition to true/false
        if ("1".equals(stringValue.trim())) {
            stringValue = "true";
        }

        return Boolean.valueOf(stringValue);
    }
}
