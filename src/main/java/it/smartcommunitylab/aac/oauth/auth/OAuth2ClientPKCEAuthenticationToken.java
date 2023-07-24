package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/*
 * A usernamePassword auth token to be used for clientId+verifier auth
 */

public class OAuth2ClientPKCEAuthenticationToken extends OAuth2ClientAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private String codeVerifier;

    private String code;

    public OAuth2ClientPKCEAuthenticationToken(
        String clientId,
        String code,
        String codeVerifier,
        String authenticationMethod
    ) {
        super(clientId);
        this.codeVerifier = codeVerifier;
        this.code = code;
        this.authenticationMethod = authenticationMethod;
        setAuthenticated(false);
    }

    public OAuth2ClientPKCEAuthenticationToken(
        String clientId,
        String code,
        String codeVerifier,
        String authenticationMethod,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(clientId, authorities);
        this.codeVerifier = codeVerifier;
        this.code = code;
        this.authenticationMethod = authenticationMethod;
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
