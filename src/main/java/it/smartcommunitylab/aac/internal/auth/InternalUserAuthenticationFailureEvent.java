package it.smartcommunitylab.aac.internal.auth;

import java.io.Serializable;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.UserAuthenticationFailureEvent;

public class InternalUserAuthenticationFailureEvent extends UserAuthenticationFailureEvent {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    // TODO evaluate adding fields for error and message

    public InternalUserAuthenticationFailureEvent(
            String authority, String provider, String realm,
            Authentication authentication, InternalAuthenticationException exception) {
        super(authority, provider, realm, exception.getSubject(), authentication, exception);

    }

    @Override
    public Map<String, Serializable> exportException() {
        Map<String, Serializable> data = super.exportException();

        InternalAuthenticationException iex = (InternalAuthenticationException) getException();
        AuthenticationException ex = iex.getException();

        data.put("error", iex.getMessage());
        data.put("errorCode", ex.getClass().getSimpleName());
        data.put("username", iex.getUsername());
        data.put("credentials", iex.getCredentials());
        data.put("flow", iex.getFlow());

        return data;
    }

    @Override
    public Map<String, Serializable> exportAuthentication() {
        Map<String, Serializable> data = super.exportAuthentication();
        Authentication auth = getAuthentication();
        InternalAuthenticationException iex = (InternalAuthenticationException) getException();

        String username = auth.getName();
        String credentials = String
                .valueOf(auth.getCredentials());

        data.put("username", username);
        data.put("credentials", credentials);
        data.put("flow", iex.getFlow());

        return data;
    }

}
