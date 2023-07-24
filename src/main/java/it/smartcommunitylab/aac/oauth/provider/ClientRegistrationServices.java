package it.smartcommunitylab.aac.oauth.provider;

import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.request.ClientRegistrationRequest;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

public interface ClientRegistrationServices {
    public ClientRegistration loadRegistrationByClientId(String clientId) throws ClientRegistrationException;

    public ClientRegistration addRegistration(String realm, ClientRegistrationRequest request)
        throws ClientRegistrationException;

    public ClientRegistration updateRegistration(String clientId, ClientRegistrationRequest request)
        throws ClientRegistrationException;

    public void removeRegistration(String clientId);
}
