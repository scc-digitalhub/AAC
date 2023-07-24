package it.smartcommunitylab.aac.openid.common.exceptions;

import it.smartcommunitylab.aac.SystemKeys;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

public class LoginRequiredException extends OAuth2Exception {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    public LoginRequiredException(String msg) {
        super(msg);
    }

    public LoginRequiredException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "login_required";
    }
}
