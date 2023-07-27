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

package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import java.util.Set;
import javax.validation.Valid;
import org.springframework.boot.context.properties.ConstructorBinding;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurableAttributeProvider extends ConfigurableProvider {

    private Set<String> attributeSets;

    private String persistence;
    private String events;

    public ConfigurableAttributeProvider(String authority, String provider, String realm) {
        super(authority, provider, realm, SystemKeys.RESOURCE_ATTRIBUTES);
        this.persistence = SystemKeys.PERSISTENCE_LEVEL_NONE;
        this.events = SystemKeys.EVENTS_LEVEL_DETAILS;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableAttributeProvider() {
        this((String) null, (String) null, (String) null);
    }

    public Set<String> getAttributeSets() {
        return attributeSets;
    }

    public void setAttributeSets(Set<String> attributeSets) {
        this.attributeSets = attributeSets;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }
}
