package it.smartcommunitylab.aac.oauth.provider;

import org.springframework.security.oauth2.provider.ClientRegistrationException;

import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.request.ClientRegistrationRequest;

public interface ClientRegistrationServices {

    public ClientRegistration loadRegistrationByClientId(String clientId) throws ClientRegistrationException;

    public ClientRegistration addRegistration(ClientRegistrationRequest request) throws ClientRegistrationException;

    public ClientRegistration updateRegistration(String clientId, ClientRegistrationRequest request)
            throws ClientRegistrationException;

    public void removeRegistration(String clientId);

}
