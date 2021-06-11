package it.smartcommunitylab.aac.core.auth;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

import it.smartcommunitylab.aac.core.ClientDetails;

public abstract class DefaultClientAuthenticationToken extends ClientAuthentication {

    // client details
    protected ClientDetails details;

    // web authentication details
    protected WebAuthenticationDetails webAuthenticationDetails;

    protected String authenticationMethod;

    public DefaultClientAuthenticationToken(String clientId) {
        super(clientId);
    }

    public DefaultClientAuthenticationToken(String clientId,
            Collection<? extends GrantedAuthority> authorities) {
        super(clientId, authorities);
    }

    @Override
    public Object getDetails() {
        return details;
    }

    public ClientDetails getClient() {
        return details;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public WebAuthenticationDetails getWebAuthenticationDetails() {
        return webAuthenticationDetails;
    }

    public void setWebAuthenticationDetails(WebAuthenticationDetails webAuthenticationDetails) {
        this.webAuthenticationDetails = webAuthenticationDetails;
    }

    public void setDetails(ClientDetails details) {
        this.details = details;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }
}
