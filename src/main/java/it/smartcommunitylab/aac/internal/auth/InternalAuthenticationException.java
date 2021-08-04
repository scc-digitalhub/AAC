package it.smartcommunitylab.aac.internal.auth;

import org.springframework.security.core.AuthenticationException;
import it.smartcommunitylab.aac.SystemKeys;

public class InternalAuthenticationException extends AuthenticationException {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String username;
    private final String credentials;
    private final String flow;
    private final AuthenticationException exception;

    public InternalAuthenticationException(String username, String message) {
        super(message);
        this.username = username;
        this.flow = null;
        this.credentials = null;
        this.exception = null;
    }

    public InternalAuthenticationException(String username, String credentials, String flow,
            AuthenticationException ex) {
        super(ex.getMessage(), ex.getCause());
        this.username = username;
        this.credentials = credentials;
        this.flow = flow;
        this.exception = ex;
    }

    public InternalAuthenticationException(String username, String credentials, String flow, AuthenticationException ex,
            String message) {
        super(message, ex.getCause());
        this.username = username;
        this.credentials = credentials;
        this.flow = flow;
        this.exception = ex;
    }

    public String getUsername() {
        return username;
    }

    public String getCredentials() {
        return credentials;
    }

    public String getFlow() {
        return flow;
    }

    public AuthenticationException getException() {
        return exception;
    }

    public String getError() {
        return exception != null ? exception.getClass().getSimpleName() : null;
    }

}
