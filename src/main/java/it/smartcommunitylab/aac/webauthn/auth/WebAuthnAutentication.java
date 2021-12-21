
package it.smartcommunitylab.aac.webauthn.auth;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnAutentication implements Authentication {

    private boolean authenticated = false;

    @Override
    public String getName() {
        // TODO: civts, Auto-generated method stub
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // TODO: civts, Auto-generated method stub
        return null;
    }

    @Override
    public WebAuthnCredential getCredentials() {
        // TODO: civts, Auto-generated method stub
        return null;
    }

    @Override
    public WebAuthnUserAccount getDetails() {
        // TODO: civts, Auto-generated method stub
        return null;
    }

    @Override
    public Object getPrincipal() {
        // TODO: civts, Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;

    }

}
