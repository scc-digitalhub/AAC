package it.smartcommunitylab.aac.oauth.request;

import java.io.Serializable;
import java.util.Map;

public interface OAuth2RegistrationRequestFactory {

    ClientRegistrationRequest createClientRegistrationRequest(Map<String, Serializable> registrationParameters);

}
