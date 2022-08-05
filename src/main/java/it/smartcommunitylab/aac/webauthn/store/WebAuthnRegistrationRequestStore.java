package it.smartcommunitylab.aac.webauthn.store;

import java.util.Collection;

import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationRequest;

public interface WebAuthnRegistrationRequestStore {

    public WebAuthnRegistrationRequest find(String key);

    public WebAuthnRegistrationRequest consume(String key);

    public Collection<WebAuthnRegistrationRequest> findAll();

    public String store(WebAuthnRegistrationRequest request);

    public void store(WebAuthnRegistrationRequest request, String key);

    public void remove(String key);

}
