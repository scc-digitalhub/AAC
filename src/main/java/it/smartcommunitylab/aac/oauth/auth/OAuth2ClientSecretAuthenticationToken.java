package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * A usernamePassword auth token to be used for clientId+secret auth
 */

public class OAuth2ClientSecretAuthenticationToken extends OAuth2ClientAuthenticationToken {

    private String credentials;

    public OAuth2ClientSecretAuthenticationToken(String clientId, String clientSecret, String authenticationMethod) {
        super(clientId);
        this.credentials = clientSecret;
        this.authenticationMethod = authenticationMethod;
        setAuthenticated(false);

    }

    public OAuth2ClientSecretAuthenticationToken(String clientId, String clientSecret, String authenticationMethod,
            Collection<? extends GrantedAuthority> authorities) {
        super(clientId, authorities);
        this.credentials = clientSecret;
        this.authenticationMethod = authenticationMethod;
    }

    @Override
    public String getCredentials() {
        return this.credentials;
    }

    public String getClientSecret() {
        return this.credentials;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
