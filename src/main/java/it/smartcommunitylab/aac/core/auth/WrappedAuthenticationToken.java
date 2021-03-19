package it.smartcommunitylab.aac.core.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public abstract class WrappedAuthenticationToken implements Authentication,
        CredentialsContainer {

    private static final long serialVersionUID = 6963872664984798641L;

    protected AbstractAuthenticationToken token;

    // audit
    protected WebAuthenticationDetails authenticationDetails;

    public WrappedAuthenticationToken(AbstractAuthenticationToken token) {
        Assert.notNull(token, "token can not be null");
        this.token = token;
    }

    public AbstractAuthenticationToken getAuthenticationToken() {
        return token;
    }

    @Override
    public boolean isAuthenticated() {
        return token.isAuthenticated();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return token.getAuthorities();
    }

    @Override
    public Object getPrincipal() {
        return token.getPrincipal();
    }

    @Override
    public Object getCredentials() {
        // no credentials exposed, refer to embedded token
        return null;
    }

    @Override
    public String getName() {
        return token.getName();
    }

    @Override
    public Object getDetails() {
        return token.getDetails();
    }

    @Override
    public void eraseCredentials() {
        token.eraseCredentials();
    }

    public WebAuthenticationDetails getAuthenticationDetails() {
        return authenticationDetails;
    }

    public void setAuthenticationDetails(WebAuthenticationDetails authenticationDetails) {
        this.authenticationDetails = authenticationDetails;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new IllegalArgumentException("Cannot set this token to trusted");
    }
}
