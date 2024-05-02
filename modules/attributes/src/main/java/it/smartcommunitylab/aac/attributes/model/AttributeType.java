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

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum AttributeType {
    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    DATE("date"),
    DATETIME("datetime"),
    TIME("time"),
    OBJECT("object");

    private final String value;

    AttributeType(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String toString() {
        return value;
    }

    public static AttributeType parse(String value) {
        for (AttributeType ct : AttributeType.values()) {
            if (ct.value.equalsIgnoreCase(value)) {
                return ct;
            }
        }

        return null;
    }
}
