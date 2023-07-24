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
