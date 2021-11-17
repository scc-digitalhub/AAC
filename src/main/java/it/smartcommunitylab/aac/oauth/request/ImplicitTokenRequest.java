package it.smartcommunitylab.aac.oauth.request;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;

public class ImplicitTokenRequest extends TokenRequest {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private final static String GRANT_TYPE = AuthorizationGrantType.IMPLICIT.getValue();

    private String redirectUri;
    private AuthorizationRequest authorizationRequest;

    protected ImplicitTokenRequest(
            Map<String, String> requestParameters,
            String clientId, Collection<String> scope,
            String redirectUri,
            Collection<String> resourceIds, Collection<String> audience) {
        super(requestParameters,
                clientId, GRANT_TYPE, scope,
                resourceIds, audience);
        this.redirectUri = redirectUri;
    }

    public ImplicitTokenRequest(
            AuthorizationRequest authorizationRequest) {
        super(authorizationRequest.getRequestParameters(),
                authorizationRequest.getClientId(), GRANT_TYPE,
                authorizationRequest.getScope(),
                authorizationRequest.getResourceIds(), null);

        // extensions
        Map<String, Serializable> extensions = authorizationRequest.getExtensions();
        if (extensions.containsKey("audience")) {
            Set<String> audience = StringUtils.commaDelimitedListToSet((String) extensions.get("audience"));
            setAudience(audience);
        }

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
    }

}
