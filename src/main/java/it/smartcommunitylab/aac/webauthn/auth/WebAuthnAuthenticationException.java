package it.smartcommunitylab.aac.webauthn.auth;

import org.springframework.security.core.AuthenticationException;

public class WebAuthnAuthenticationException extends AuthenticationException {

    public WebAuthnAuthenticationException(String msg) {
        super(msg);
    }

}
