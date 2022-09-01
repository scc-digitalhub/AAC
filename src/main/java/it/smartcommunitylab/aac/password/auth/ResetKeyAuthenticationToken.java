package it.smartcommunitylab.aac.password.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public class ResetKeyAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String username;
    private String key;

    private InternalUserAccount account;

    public ResetKeyAuthenticationToken(String username, String key) {
        super(null);
        this.username = username;
        this.key = key;
        setAuthenticated(false);
    }

    public ResetKeyAuthenticationToken(String username, String key,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.username = username;
        this.key = key;
        super.setAuthenticated(true);
    }

    public ResetKeyAuthenticationToken(String username, String key, InternalUserAccount account,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.username = username;
        this.key = key;
        this.account = account;
        super.setAuthenticated(true);
    }

    public String getUsername() {
        return username;
    }

    public String getKey() {
        return key;
    }

    public InternalUserAccount getAccount() {
        return account;
    }

    @Override
    public Object getCredentials() {
        return this.key;
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
        this.key = null;
        if (this.account != null) {
            this.account.eraseCredentials();
        }
    }
}
