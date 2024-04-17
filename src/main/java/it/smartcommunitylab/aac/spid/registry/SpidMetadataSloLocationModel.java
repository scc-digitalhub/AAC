/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpidMetadataSloLocationModel {

    @JsonProperty("Binding")
    private String Binding;

    @JsonProperty("Location")
    private String Location;

    public SpidMetadataSloLocationModel() {}

    public SpidMetadataSloLocationModel(String binding, String location) {
        Binding = binding;
        Location = location;
    }

    public String getBinding() {
        return Binding;
    }

    public String getLocation() {
        return Location;
    }

    @Override
    public String toString() {
        return "SpidSloLocationModel{" + "Binding='" + Binding + '\'' + ", Location='" + Location + '\'' + '}';
    }
}
