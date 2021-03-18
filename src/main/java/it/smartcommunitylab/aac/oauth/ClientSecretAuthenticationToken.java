package it.smartcommunitylab.aac.oauth;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/*
 * A usernamePassword auth token to be used for clientId+secret auth
 */

public class ClientSecretAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public ClientSecretAuthenticationToken(String clientId, String clientSecret) {
        super(clientId, clientSecret);
    }

    public ClientSecretAuthenticationToken(String clientId, String clientSecret,
            Collection<? extends GrantedAuthority> authorities) {
        super(clientId, clientSecret, authorities);
    }
}
