package it.smartcommunitylab.aac.oauth.request;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;

public class AuthorizationCodeTokenRequest extends TokenRequest {
    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private final static String GRANT_TYPE = AuthorizationGrantType.AUTHORIZATION_CODE.getValue();

    private String code;
    private String redirectUri;

    private AuthorizationRequest authorizationRequest;

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

    public AuthorizationRequest getAuthorizationRequest() {
        return authorizationRequest;
    }

    public void setAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        this.authorizationRequest = authorizationRequest;
        // also update params from authorizationRequest
        setScope(authorizationRequest.getScope());
        setResourceIds(authorizationRequest.getResourceIds());
        setRedirectUri(authorizationRequest.getRedirectUri());

        // extensions
        Map<String, Serializable> extensions = authorizationRequest.getExtensions();
        if (extensions.containsKey("audience")) {
            Set<String> audience = StringUtils.commaDelimitedListToSet((String) extensions.get("audience"));
            setAudience(audience);
        }
        if (extensions.containsKey("nonce")) {
            getExtensions().put("nonce", extensions.get("nonce"));
        }
    }

}
