package it.smartcommunitylab.aac.oauth.request;

import java.util.Collection;

import org.springframework.security.oauth2.provider.AuthorizationRequest;

public interface OAuth2AuthorizationRequestRepository {

    public AuthorizationRequest find(String key);

    public Collection<AuthorizationRequest> findAll();

    public String store(AuthorizationRequest request);

    public void store(AuthorizationRequest request, String key);

    public void remove(String key);

}
