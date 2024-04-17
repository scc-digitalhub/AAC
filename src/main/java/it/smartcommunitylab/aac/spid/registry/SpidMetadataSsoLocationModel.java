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

public class SpidMetadataSsoLocationModel {

    @JsonProperty("Binding")
    private String binding;

    @JsonProperty("Location")
    private String location;

    public SpidMetadataSsoLocationModel() {}

    public SpidMetadataSsoLocationModel(String binding, String location) {
        this.binding = binding;
        this.location = location;
    }

    public String getBinding() {
        return binding;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "SpidSsoLocationModel{" + "binding='" + binding + '\'' + ", location='" + location + '\'' + '}';
    }
}
