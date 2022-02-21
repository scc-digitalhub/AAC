package it.smartcommunitylab.aac.webauthn.auth;


import org.springframework.security.core.AuthenticationException;

import it.smartcommunitylab.aac.SystemKeys;

public class WebAuthnAuthenticationException extends AuthenticationException {
 
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String subject;
    private final String username;
    private final String credentials; 
    private final AuthenticationException exception;

    public WebAuthnAuthenticationException(String subject, String message) {
        super(message);
        this.subject = subject;
        this.username = null; 
        this.credentials = null;
        this.exception = null;
    }

    public WebAuthnAuthenticationException(String subject, String username, String credentials,
            AuthenticationException ex) {
        super(ex.getMessage(), ex.getCause());
        this.subject = subject;
        this.username = username;
        this.credentials = credentials; 
        this.exception = ex;
    }

    public WebAuthnAuthenticationException(String subject, String username, String credentials,
            AuthenticationException ex,
            String message) {
        super(message, ex.getCause());
        this.subject = subject;
        this.username = username;
        this.credentials = credentials; 
        this.exception = ex;
    }

    public String getSubject() {
        return subject;
    }

    public String getUsername() {
        return username;
    }

    public String getCredentials() {
        return credentials;
    }
 
    public AuthenticationException getException() {
        return exception;
    }

    public String getError() {
        return exception != null ? exception.getClass().getSimpleName() : null;
    }

    public String getErrorMessage() {
        String error = getError();
        if (error == null) {
            return "internal_error";
        }
        return "error." + error;
    } 
}
