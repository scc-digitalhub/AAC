package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public abstract class OAuth2ClientAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = -2640624036104536421L;

    // clientId is principal
    private final String principal;

    // keep realm separated to support clients authentication in different realms
    protected String realm;

    // client details
    protected OAuth2ClientDetails details;

    // web authentication details
    protected WebAuthenticationDetails webAuthenticationDetails;

    protected String authenticationScheme;

    public OAuth2ClientAuthenticationToken(String clientId) {
        super(null);
        this.principal = clientId;
        setAuthenticated(false);
    }

    public OAuth2ClientAuthenticationToken(String clientId,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = clientId;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public String getCredentials() {
        return null;
    }

    @Override
    public String getPrincipal() {
        return this.principal;
    }

    @Override
    public Object getDetails() {
        return details;
    }

    public OAuth2ClientDetails getClient() {
        return details;
    }

    public String getClientId() {
        return this.principal;
    }

    public String getRealm() {
        return realm;
    }

    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }

        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        // nothing to do
    }

    public WebAuthenticationDetails getWebAuthenticationDetails() {
        return webAuthenticationDetails;
    }

    public void setWebAuthenticationDetails(WebAuthenticationDetails webAuthenticationDetails) {
        this.webAuthenticationDetails = webAuthenticationDetails;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setDetails(OAuth2ClientDetails details) {
        this.details = details;
    }

    public void setAuthenticationScheme(String authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
    }

}
