package it.smartcommunitylab.aac.oauth.request;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;

public class ImplicitTokenRequest extends TokenRequest {
    private final static String GRANT_TYPE = AuthorizationGrantType.IMPLICIT.getValue();

    private String redirectUri;

    public ImplicitTokenRequest(
            Map<String, String> requestParameters,
            String clientId,
            String redirectUri,
            Collection<String> resourceIds, Collection<String> audience) {
        super(requestParameters,
                clientId, GRANT_TYPE, Collections.emptyList(),
                resourceIds, audience);

        this.redirectUri = redirectUri;

    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

}
