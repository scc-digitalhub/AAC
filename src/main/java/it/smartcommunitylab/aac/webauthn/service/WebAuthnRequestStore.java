package it.smartcommunitylab.aac.webauthn.service;

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
 * TODO replace with interface + in-memory implementation
 */
public class WebAuthnRequestStore {

    private final Map<String, AssertionRequest> requests;

    public WebAuthnRequestStore() {
        this.requests = new ConcurrentHashMap<>();
    }

    public AssertionRequest find(String key) {
        Assert.hasText(key, "key can not be null or empty");
        return requests.get(key);
    }

    public AssertionRequest consume(String key) {
        Assert.hasText(key, "key can not be null or empty");
        return requests.remove(key);
    }

    public Collection<AssertionRequest> findAll() {
        return Collections.unmodifiableCollection(requests.values());
    }

    public String store(AssertionRequest request) {
        String key = UUID.randomUUID().toString();
        requests.put(key, request);
        return key;
    }

    public void store(String key, AssertionRequest request) {
        requests.put(key, request);
    }

    public void remove(String key) {
        requests.remove(key);
    }

}
