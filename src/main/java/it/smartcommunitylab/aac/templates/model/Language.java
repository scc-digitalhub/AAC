/**
 * Copyright 2023 Fondazione Bruno Kessler
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

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum Language {
    IT("it"),
    EN("en"),
    DE("de"),
    ES("es"),
    LV("lv");

    private final String value;

    Language(String value) {
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

    public static Language parse(String value) {
        for (Language t : Language.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
