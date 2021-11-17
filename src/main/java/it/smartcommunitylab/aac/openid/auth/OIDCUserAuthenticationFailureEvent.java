package it.smartcommunitylab.aac.openid.auth;

import java.io.Serializable;
import java.util.Map;

import org.springframework.security.core.Authentication;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.UserAuthenticationFailureEvent;

public class OIDCUserAuthenticationFailureEvent extends UserAuthenticationFailureEvent {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    // TODO evaluate adding fields for error and message

    public OIDCUserAuthenticationFailureEvent(
            String authority, String provider, String realm,
            Authentication authentication, OIDCAuthenticationException exception) {
        super(authority, provider, realm, null, authentication, exception);

    }

    @Override
    public Map<String, Serializable> exportException() {
        Map<String, Serializable> data = super.exportException();

        OIDCAuthenticationException ex = (OIDCAuthenticationException) getException();
        data.put("error", ex.getError().getDescription());
        data.put("errorCode", ex.getError().getErrorCode());
        data.put("authorizationRequest", ex.getAuthorizationRequest());
        data.put("authorizationResponse", ex.getAuthorizationResponse());
        data.put("tokenRequest", ex.getTokenRequest());
        data.put("tokenResponse", ex.getTokenResponse());

        return data;
    }

}
