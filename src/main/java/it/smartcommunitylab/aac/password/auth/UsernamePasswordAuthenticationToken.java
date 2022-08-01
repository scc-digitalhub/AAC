package it.smartcommunitylab.aac.password.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public class UsernamePasswordAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String username;
    private String password;

    private InternalUserAccount account;

    public UsernamePasswordAuthenticationToken(String username, String password) {
        super(null);
        this.username = username;
        this.password = password;
        setAuthenticated(false);
    }

    public UsernamePasswordAuthenticationToken(String username, String password,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.username = username;
        this.password = password;
        super.setAuthenticated(true);
    }

    public UsernamePasswordAuthenticationToken(String username, String password, InternalUserAccount account,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.username = username;
        this.password = password;
        this.account = account;
        super.setAuthenticated(true);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public InternalUserAccount getAccount() {
        return account;
    }

    @Override
    public Object getCredentials() {
        return this.password;
    }

    @Override
    public Object getPrincipal() {
        return (this.account == null ? this.username : this.account);
    }

    @Override
    public String getName() {
        return this.username;
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
        this.password = null;
        if (this.account != null) {
            this.account.eraseCredentials();
        }
    }
}
