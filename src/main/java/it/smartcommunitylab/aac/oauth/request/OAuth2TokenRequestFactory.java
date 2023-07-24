package it.smartcommunitylab.aac.oauth.request;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import java.util.Map;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Request;

public interface OAuth2TokenRequestFactory {
    TokenRequest createTokenRequest(Map<String, String> requestParameters, OAuth2ClientDetails clientDetails);

    TokenRequest createTokenRequest(AuthorizationRequest authorizationRequest, String grantType);

    OAuth2Request createOAuth2Request(TokenRequest tokenRequest, OAuth2ClientDetails client);
}
