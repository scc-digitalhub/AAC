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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@JsonInclude(Include.NON_EMPTY)
public class CustomProfile extends AbstractProfile {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    @JsonIgnore
    private final String identifier;

    // attributes map should be kept internal, anyGetter will ensure values are
    // extracted one by one. jsonUnwrapped does not work for convertValue
    @JsonIgnore
    private final Map<String, Serializable> attributes;

    public CustomProfile(String id) {
        Assert.hasText(id, "identifier can not be null or empty");
        String identifier = id;
        if (!identifier.startsWith("profile.")) {
            identifier = "profile." + identifier;
        }

        this.identifier = identifier;
        this.attributes = new HashMap<>();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @JsonAnyGetter
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    public void addAttribute(String key, Serializable value) {
        if (StringUtils.hasText(key)) {
            this.attributes.put(key, value);
        }
    }
}
