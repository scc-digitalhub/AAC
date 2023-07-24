package it.smartcommunitylab.aac.openid.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

public class OIDCAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    private final String subject;

    private OAuth2User principal;

    private OAuth2AccessToken accessToken;

    private OAuth2RefreshToken refreshToken;

    public OIDCAuthenticationToken(String subject) {
        super(null);
        this.subject = subject;
        this.setAuthenticated(false);
    }

    public OIDCAuthenticationToken(
        String subject,
        OAuth2User principal,
        OAuth2AccessToken accessToken,
        OAuth2RefreshToken refreshToken,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        Assert.hasText(subject, "sub can not be null or empty");
        Assert.notNull(principal, "principal cannot be null");
        Assert.notNull(accessToken, "accessToken cannot be null");
        this.subject = subject;
        this.principal = principal;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.setAuthenticated(true);
    }

    @Override
    public OAuth2User getPrincipal() {
        return this.principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    public String getSubject() {
        return subject;
    }

    public OAuth2AccessToken getAccessToken() {
        return accessToken;
    }

    public OAuth2RefreshToken getRefreshToken() {
        return refreshToken;
    }
}
