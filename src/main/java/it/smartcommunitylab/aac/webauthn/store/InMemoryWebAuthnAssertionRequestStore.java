package it.smartcommunitylab.aac.webauthn.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

import com.yubico.webauthn.AssertionRequest;

/*
 * In memory local request store
 * 
 */
public class InMemoryWebAuthnAssertionRequestStore implements WebAuthnAssertionRequestStore {

    private final Map<String, AssertionRequest> requests;

    public InMemoryWebAuthnAssertionRequestStore() {
        this.requests = new ConcurrentHashMap<>();
    }

    @Override
    public AssertionRequest find(String key) {
        Assert.hasText(key, "key can not be null or empty");
        return requests.get(key);
    }

    @Override
    public AssertionRequest consume(String key) {
        Assert.hasText(key, "key can not be null or empty");
        return requests.remove(key);
    }

    @Override
    public Collection<AssertionRequest> findAll() {
        return Collections.unmodifiableCollection(requests.values());
    }

    @Override
    public String store(AssertionRequest request) {
        String key = extractKey(request);
        requests.put(key, request);
        return key;
    }

    @Override
    public void store(AssertionRequest request, String key) {
        requests.put(key, request);
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
