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

package it.smartcommunitylab.aac.profiles.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import java.util.HashMap;

@JsonInclude(Include.NON_EMPTY)
public abstract class AbstractProfile implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<
        HashMap<String, Serializable>
    >() {};

    static {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.registerModule(new JavaTimeModule());
    }

    public String getId() {
        StringBuilder sb = new StringBuilder();
        sb.append(getIdentifier());
        return sb.toString();
    }

    @JsonIgnore
    public abstract String getIdentifier();

    /*
     * Convert profile, subclasses can override
     */

    @JsonIgnore
    public String toJson() throws IllegalArgumentException {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @JsonIgnore
    public HashMap<String, Serializable> toMap() throws IllegalArgumentException {
        try {
            return mapper.convertValue(this, typeRef);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
