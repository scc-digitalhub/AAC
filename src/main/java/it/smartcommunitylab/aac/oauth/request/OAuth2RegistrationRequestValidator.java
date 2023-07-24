package it.smartcommunitylab.aac.oauth.request;

import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;

public interface OAuth2RegistrationRequestValidator {
    public void validate(ClientRegistrationRequest registrationRequest) throws InvalidRequestException;
}
