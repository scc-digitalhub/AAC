package it.smartcommunitylab.aac.core.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public abstract class ClientAuthenticationProvider implements AuthenticationProvider {

    @Override
    public abstract ClientAuthentication authenticate(Authentication authentication)
            throws AuthenticationException;

}
