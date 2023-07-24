package it.smartcommunitylab.aac.webauthn.auth;

import it.smartcommunitylab.aac.SystemKeys;
import org.springframework.security.core.AuthenticationException;

public class WebAuthnAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private final String subject;
    private final String username;
    private final String assertion;
    private final AuthenticationException exception;

    public WebAuthnAuthenticationException(String subject, String message) {
        super(message);
        this.subject = subject;
        this.username = null;
        this.assertion = null;
        this.exception = null;
    }

    public WebAuthnAuthenticationException(
        String subject,
        String username,
        String assertion,
        AuthenticationException ex
    ) {
        super(ex.getMessage(), ex.getCause());
        this.subject = subject;
        this.username = username;
        this.assertion = assertion;
        this.exception = ex;
    }

    public WebAuthnAuthenticationException(
        String subject,
        String username,
        String assertion,
        AuthenticationException ex,
        String message
    ) {
        super(message, ex.getCause());
        this.subject = subject;
        this.username = username;
        this.assertion = assertion;
        this.exception = ex;
    }

    public String getSubject() {
        return subject;
    }

    public String getUsername() {
        return username;
    }

    public String getAssertion() {
        return assertion;
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
            return "webauthn_error";
        }
        return "error." + error;
    }
}
