package it.smartcommunitylab.aac.oauth.request;

import java.util.Map;

import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Request;

import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public interface OAuth2AuthorizationRequestFactory {
    AuthorizationRequest createAuthorizationRequest(Map<String, String> authorizationParameters,
            OAuth2ClientDetails clientDetails, User user);

    OAuth2Request createOAuth2Request(AuthorizationRequest request, OAuth2ClientDetails clientDetails);

}
