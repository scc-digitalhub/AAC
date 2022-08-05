package it.smartcommunitylab.aac.webauthn.store;

import java.util.Collection;

import com.yubico.webauthn.AssertionRequest;

public interface WebAuthnAssertionRequestStore {

    public AssertionRequest find(String key);

    public AssertionRequest consume(String key);

    public Collection<AssertionRequest> findAll();

    public String store(AssertionRequest request);

    public void store(AssertionRequest request, String key);

    public void remove(String key);

}
