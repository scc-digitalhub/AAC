package it.smartcommunitylab.aac.oauth.store;

import java.util.Collection;
import org.springframework.security.oauth2.provider.AuthorizationRequest;

public interface AuthorizationRequestStore {
    public AuthorizationRequest find(String key);

    public Collection<AuthorizationRequest> findAll();

    public String store(AuthorizationRequest request);

    public void store(AuthorizationRequest request, String key);

    public void remove(String key);
}
