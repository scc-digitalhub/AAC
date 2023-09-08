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

package it.smartcommunitylab.aac.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class AttributeSetsProperties {

    @NestedConfigurationProperty
    private List<AttributeSetDefinition> sets;

    public AttributeSetsProperties() {
        sets = new ArrayList<>();
    }

    public List<AttributeSetDefinition> getSets() {
        return sets;
    }

    public void setSets(List<AttributeSetDefinition> sets) {
        this.sets = sets;
    }

    public static class AttributeSetDefinition {

        @NotBlank
        private String identifier;

        @NotNull
        private String[] keys;

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String[] getKeys() {
            return keys;
        }

        public void setKeys(String[] keys) {
            this.keys = keys;
        }
    }
}
