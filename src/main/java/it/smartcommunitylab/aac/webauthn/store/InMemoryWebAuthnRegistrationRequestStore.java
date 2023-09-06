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

import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.Assert;

/*
 * In memory local request store
 *
 */
public class InMemoryWebAuthnRegistrationRequestStore implements WebAuthnRegistrationRequestStore {

    private final Map<String, WebAuthnRegistrationRequest> requests;

    public InMemoryWebAuthnRegistrationRequestStore() {
        this.requests = new ConcurrentHashMap<>();
    }

    @Override
    public WebAuthnRegistrationRequest find(String key) {
        Assert.hasText(key, "key can not be null or empty");
        return requests.get(key);
    }

    @Override
    public WebAuthnRegistrationRequest consume(String key) {
        Assert.hasText(key, "key can not be null or empty");
        return requests.remove(key);
    }

    @Override
    public Collection<WebAuthnRegistrationRequest> findAll() {
        return Collections.unmodifiableCollection(requests.values());
    }

    @Override
    public String store(WebAuthnRegistrationRequest request) {
        String key = extractKey(request);
        requests.put(key, request);
        return key;
    }

    @Override
    public void store(WebAuthnRegistrationRequest request, String key) {
        requests.put(key, request);
    }

    @Override
    public void remove(String key) {
        requests.remove(key);
    }

    private String extractKey(WebAuthnRegistrationRequest request) {
        // TODO evaluate consistent hashing for key generation
        return UUID.randomUUID().toString();
    }
}
