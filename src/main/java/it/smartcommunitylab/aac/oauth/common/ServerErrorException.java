package it.smartcommunitylab.aac.oauth.common;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

public class ServerErrorException extends OAuth2Exception {

    public ServerErrorException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "server_error";
    }

    @Override
    public int getHttpErrorCode() {
        return 500;
    }

}
