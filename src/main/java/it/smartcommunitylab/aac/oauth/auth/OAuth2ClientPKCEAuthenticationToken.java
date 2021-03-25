package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * A usernamePassword auth token to be used for clientId+verifier auth
 */

public class OAuth2ClientPKCEAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;

    private String codeVerifier;

    private String code;

    public OAuth2ClientPKCEAuthenticationToken(String clientId, String code, String codeVerifier) {
        super(null);
        this.principal = clientId;
        this.codeVerifier = codeVerifier;
        this.code = code;
        setAuthenticated(false);
    }

    public OAuth2ClientPKCEAuthenticationToken(String clientId, String code, String codeVerifier,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = clientId;
        this.codeVerifier = codeVerifier;
        this.code = code;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public String getCredentials() {
        return this.codeVerifier;
    }

    @Override
    public String getPrincipal() {
        return this.principal;
    }

    public String getCode() {
        return this.code;
    }

    public String getCodeVerifier() {
        return this.codeVerifier;
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
        this.codeVerifier = null;
        this.code = null;
    }

}
