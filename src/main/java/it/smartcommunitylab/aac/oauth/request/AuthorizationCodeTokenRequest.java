package it.smartcommunitylab.aac.oauth.request;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;

public class AuthorizationCodeTokenRequest extends TokenRequest {
    private final static String GRANT_TYPE = AuthorizationGrantType.AUTHORIZATION_CODE.getValue();

    private String code;
    private String redirectUri;

    // TODO add field for authorizationRequest
//    private AuthorizationRequest authorizationRequest;

    public AuthorizationCodeTokenRequest(
            Map<String, String> requestParameters,
            String clientId,
            String code, String redirectUri,
            Collection<String> resourceIds, Collection<String> audience) {
        super(requestParameters,
                clientId, GRANT_TYPE, Collections.emptyList(),
                resourceIds, audience);
        Assert.hasText(code, "code is required");

        this.code = code;
        this.redirectUri = redirectUri;

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

}
