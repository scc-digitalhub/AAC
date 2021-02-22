package it.smartcommunitylab.aac.core;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class RealmAwareUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private static final long serialVersionUID = -3312307110804452724L;

    private final String realm;

    public RealmAwareUsernamePasswordAuthenticationToken(String realm, Object principal, Object credentials) {
        super(principal, credentials);
        this.realm = realm;
    }

    public RealmAwareUsernamePasswordAuthenticationToken(String realm, Object principal, Object credentials,
            Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            // we want a full authentication token, can't trust this via method
            // use the full constructor
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted");
        }
    }

}
