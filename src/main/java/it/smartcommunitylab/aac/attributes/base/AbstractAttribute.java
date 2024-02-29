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

package it.smartcommunitylab.aac.attributes.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.Attribute;
import java.io.Serializable;

public abstract class AbstractAttribute implements Attribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    protected String key;

    protected String name;
    protected String description;

    @JsonProperty("multiple")
    private Boolean isMultiple;

    protected Boolean isRequired = false;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String exportValue() {
        Serializable value = getValue();
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsMultiple() {
        return isMultiple;
    }

    public void setIsMultiple(Boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    public Boolean getIsRequired() {
        return this.isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
}
