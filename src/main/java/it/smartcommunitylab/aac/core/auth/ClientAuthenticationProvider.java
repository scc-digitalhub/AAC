package it.smartcommunitylab.aac.core.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import it.smartcommunitylab.aac.core.service.ClientDetailsService;

public abstract class ClientAuthenticationProvider implements AuthenticationProvider {

    protected ClientDetailsService clientService;

    @Override
    public abstract ClientAuthentication authenticate(Authentication authentication)
            throws AuthenticationException;

    public void setClientService(ClientDetailsService clientService) {
        this.clientService = clientService;
    }

}
