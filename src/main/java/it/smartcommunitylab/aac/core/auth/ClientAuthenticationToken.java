package it.smartcommunitylab.aac.core.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

public abstract class ClientAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = -2640624036104536421L;

    // clientId is principal
    private final String principal;

    // keep realm separated to support clients authentication in different realms
    protected String realm;

    // client details
    protected ClientDetails details;

    // web authentication details
    protected WebAuthenticationDetails webAuthenticationDetails;

    protected String authenticationMethod;

    public ClientAuthenticationToken(String clientId) {
        super(null);
        this.principal = clientId;
        setAuthenticated(false);
    }

    public ClientAuthenticationToken(String clientId,
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

    @Override
    public String getName() {
        return principal;
    }

    public ClientDetails getClient() {
        return details;
    }

    public String getClientId() {
        return this.principal;
    }

    public String getRealm() {
        return realm;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
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

    public void setDetails(ClientDetails details) {
        this.details = details;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

}
