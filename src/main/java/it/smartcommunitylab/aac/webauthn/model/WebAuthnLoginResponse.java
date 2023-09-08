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

package it.smartcommunitylab.aac.webauthn.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnLoginResponse {

    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("key")
    @NotNull
    private String key;

    @NotNull
    private AssertionRequest assertionRequest;

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public AssertionRequest getAssertionRequest() {
        return this.assertionRequest;
    }

    public void setAssertionRequest(AssertionRequest assertionrequest) {
        this.assertionRequest = assertionrequest;
    }

    @JsonGetter("assertionRequest")
    public JsonNode getOptionsAsJson() throws JsonProcessingException {
        return mapper.readTree(assertionRequest.toCredentialsGetJson());
    }
}
