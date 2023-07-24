package it.smartcommunitylab.aac.openid.common.exceptions;

import it.smartcommunitylab.aac.SystemKeys;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

public class UnsupportedRegistrationException extends OAuth2Exception {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    public UnsupportedRegistrationException(String msg) {
        super(msg);
    }

    public UnsupportedRegistrationException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "registration_not_supported";
    }
}
