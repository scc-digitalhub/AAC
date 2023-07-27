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

package it.smartcommunitylab.aac.oauth.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/*
 * Additional information holder with mapping
 *
 * Stores extra information about oauth2 clients
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2ClientInfo implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();

    // only string properties, otherwise change method signatures to accept
    // serializable objects
    @JsonProperty("display_name")
    private String displayName;

    // TODO add extra configuration
    //    static {
    //        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //    }

    @SuppressWarnings("unchecked")
    public static Map<String, Serializable> read(String additionalInformation) {
        try {
            return mapper.readValue(additionalInformation, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static OAuth2ClientInfo convert(Map<String, Serializable> map) {
        return mapper.convertValue(map, OAuth2ClientInfo.class);
    }

    public String toJson() throws IllegalArgumentException {
        try {
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Serializable> toMap() throws IllegalArgumentException {
        try {
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            return mapper.convertValue(this, HashMap.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
