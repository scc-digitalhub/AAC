package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * A usernamePassword auth token to be used for clientId+secret auth
 */

public class OAuth2ClientSecretAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;
    private String credentials;
    private final String authenticationScheme;

    public OAuth2ClientSecretAuthenticationToken(String clientId, String clientSecret, String authenticationScheme) {
        super(null);
        this.principal = clientId;
        this.credentials = clientSecret;
        this.authenticationScheme = authenticationScheme;
        setAuthenticated(false);

    }

    public OAuth2ClientSecretAuthenticationToken(String clientId, String clientSecret, String authenticationScheme,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = clientId;
        this.credentials = clientSecret;
        this.authenticationScheme = authenticationScheme;
        super.setAuthenticated(true); // must use super, as we override

    }

    @Override
    public String getCredentials() {
        return this.credentials;
    }

    @Override
    public String getPrincipal() {
        return this.principal;
    }

    public String getClientId() {
        return this.principal;
    }

    public String getClientSecret() {
        return this.credentials;
    }

    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        Assert.isTrue(!isAuthenticated,
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
