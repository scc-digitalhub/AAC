package it.smartcommunitylab.aac.otp.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class UsernameOtpAuthenticationToken extends AbstractAuthenticationToken {

    private final String username;
    private String otp;
    private InternalUserAccount account;

    public UsernameOtpAuthenticationToken(String username, String otp) {
        super(null);
        this.username = username;
        this.otp = otp;
        setAuthenticated(false);
    }

    public UsernameOtpAuthenticationToken(
        String username,
        String otp,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.username = username;
        this.otp = otp;
        super.setAuthenticated(true);
    }

    public UsernameOtpAuthenticationToken(
        String username,
        String otp,
        InternalUserAccount account,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.username = username;
        this.otp = otp;
        this.account = account;
        super.setAuthenticated(true);
    }

    public String getUsername() {
        return username;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    @JsonIgnore
    public InternalUserAccount getAccount() {
        return account;
    }

    @Override
    public Object getCredentials() {
        return this.otp;
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
        Assert.isTrue(
            !isAuthenticated,
            "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead"
        );
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.otp = null;
        if (this.account != null) {
            this.account.eraseCredentials();
        }
    }
}
