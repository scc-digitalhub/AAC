package it.smartcommunitylab.aac.webauthn.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class RegisterCredentialAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String username;

    private WebAuthnUserAccount account;

    public RegisterCredentialAuthenticationToken(String username) {
        super(null);
        this.username = username;
        setAuthenticated(false);
    }

    public RegisterCredentialAuthenticationToken(String username,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.username = username;
        super.setAuthenticated(true);
    }

    public RegisterCredentialAuthenticationToken(String username, WebAuthnUserAccount account,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.username = username;
        this.account = account;
        super.setAuthenticated(true);
    }

    public String getUsername() {
        return username;
    }

    public WebAuthnUserAccount getAccount() {
        return account;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return (this.account == null ? this.username : this.account);
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
        this.account = null;
    }
}
