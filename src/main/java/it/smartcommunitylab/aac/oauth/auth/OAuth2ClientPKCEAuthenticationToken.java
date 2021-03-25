package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.oauth.model.AuthenticationScheme;

/*
 * A usernamePassword auth token to be used for clientId+verifier auth
 */

public class OAuth2ClientPKCEAuthenticationToken extends OAuth2ClientAuthenticationToken {

    private String codeVerifier;

    private String code;

    public OAuth2ClientPKCEAuthenticationToken(String clientId, String code, String codeVerifier,
            String authenticationScheme) {
        super(clientId);
        this.codeVerifier = codeVerifier;
        this.code = code;
        this.authenticationScheme = authenticationScheme;
        setAuthenticated(false);
    }

    public OAuth2ClientPKCEAuthenticationToken(String clientId, String code, String codeVerifier,
            String authenticationScheme,
            Collection<? extends GrantedAuthority> authorities) {
        super(clientId, authorities);
        this.codeVerifier = codeVerifier;
        this.code = code;
        this.authenticationScheme = authenticationScheme;

        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public String getCredentials() {
        return this.codeVerifier;
    }

    public String getCode() {
        return this.code;
    }

    public String getCodeVerifier() {
        return this.codeVerifier;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.codeVerifier = null;
        this.code = null;
    }

}
