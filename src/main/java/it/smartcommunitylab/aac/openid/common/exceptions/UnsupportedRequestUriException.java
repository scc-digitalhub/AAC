package it.smartcommunitylab.aac.openid.common.exceptions;

import it.smartcommunitylab.aac.SystemKeys;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

public class UnsupportedRequestUriException extends OAuth2Exception {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    public UnsupportedRequestUriException(String msg) {
        super(msg);
    }

    public UnsupportedRequestUriException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "request_uri_not_supported";
    }
}
