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

package it.smartcommunitylab.aac.webauthn.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.AssertionRequest;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

/*
 * In memory local request store
 *
 */
public class InMemoryWebAuthnAssertionRequestStore implements WebAuthnAssertionRequestStore, Serializable {

    // private final Map<String, AssertionRequest> requests;
    //use json string since requests are not serializable
    private final Map<String, String> requests;

    private AssertionRequest from(String json) {
        try {
            return AssertionRequest.fromJson(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String to(AssertionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException();
        }
        try {
            return request.toJson();
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public InMemoryWebAuthnAssertionRequestStore() {
        this.requests = new ConcurrentHashMap<>();
    }

    @Override
    public AssertionRequest find(String key) {
        Assert.hasText(key, "key can not be null or empty");
        if (requests.containsKey(key)) {
            return from(requests.get(key));
        }

        return null;
    }

    @Override
    public AssertionRequest consume(String key) {
        Assert.hasText(key, "key can not be null or empty");
        if (requests.containsKey(key)) {
            return from(requests.remove(key));
        }

        return null;
    }

    @Override
    public Collection<AssertionRequest> findAll() {
        return Collections.unmodifiableCollection(
            requests.values().stream().map(j -> from(j)).collect(Collectors.toList())
        );
    }

    @Override
    public String store(AssertionRequest request) {
        String key = extractKey(request);
        String json = to(request);
        requests.put(key, json);
        return key;
    }

    @Override
    public void store(AssertionRequest request, String key) {
        requests.put(key, to(request));
    }

    @Override
    public void remove(String key) {
        requests.remove(key);
    }

    private String extractKey(AssertionRequest request) {
        // TODO evaluate consistent hashing for key generation
        return UUID.randomUUID().toString();
    }
}
