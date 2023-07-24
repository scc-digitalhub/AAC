package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/*
 * A usernamePassword auth token to be used for clientId+verifier auth
 */

public class OAuth2ClientRefreshAuthenticationToken extends OAuth2ClientAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private String refreshToken;

    public OAuth2ClientRefreshAuthenticationToken(String clientId, String refreshToken, String authenticationMethod) {
        super(clientId);
        this.refreshToken = refreshToken;
        this.authenticationMethod = authenticationMethod;
        setAuthenticated(false);
    }

    public OAuth2ClientRefreshAuthenticationToken(
        String clientId,
        String refreshToken,
        String authenticationMethod,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(clientId, authorities);
        this.refreshToken = refreshToken;
        this.authenticationMethod = authenticationMethod;
    }

    @Override
    public String getCredentials() {
        return this.refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.refreshToken = null;
    }
}
