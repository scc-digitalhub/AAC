package it.smartcommunitylab.aac.oauth.request;

import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public interface OAuth2TokenRequestValidator {

    public void validate(String realm, TokenRequest tokenRequest, OAuth2ClientDetails clientDetails)
            throws InvalidRequestException;

//    public void validateScope(TokenRequest tokenRequest, OAuth2ClientDetails client) throws InvalidScopeException;

}
