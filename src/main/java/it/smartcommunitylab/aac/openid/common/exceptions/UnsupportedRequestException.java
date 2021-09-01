package it.smartcommunitylab.aac.openid.common.exceptions;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import it.smartcommunitylab.aac.SystemKeys;

public class UnsupportedRequestException extends OAuth2Exception {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    public UnsupportedRequestException(String msg) {
        super(msg);
    }

    public UnsupportedRequestException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "request_not_supported";
    }

}
