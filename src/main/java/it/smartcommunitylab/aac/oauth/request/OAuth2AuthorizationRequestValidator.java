package it.smartcommunitylab.aac.oauth.request;

import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;

import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public interface OAuth2AuthorizationRequestValidator {

    public void validate(AuthorizationRequest authorizationRequest, OAuth2ClientDetails clientDetails, User user)
            throws InvalidRequestException;

    public void validateScope(AuthorizationRequest authorizationRequest, OAuth2ClientDetails client)
            throws InvalidScopeException;

}
