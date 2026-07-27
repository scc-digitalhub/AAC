package it.smartcommunitylab.aac.otp.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**
 * Authentication token for OTP-based requests.
 * Holds user credentials (username and OTP) or account details upon successful
 * authentication.
 */
public class UsernameOtpAuthenticationToken extends AbstractAuthenticationToken {

    private final String username;
    private String otp;

    private InternalUserAccount account;

    /**
     * Constructs an unauthenticated token with username and OTP code.
     *
     * @param username The identifier of the user.
     * @param otp      The one-time code or magic link token.
     */
    public UsernameOtpAuthenticationToken(String username, String otp) {
        super(null);
        this.username = username;
        this.otp = otp;
        // Mark as unauthenticated until verification completes
        setAuthenticated(false);
    }

    /**
     * Constructs a trusted token with authorities.
     *
     * @param username    The identifier of the user.
     * @param otp         The one-time code or magic link token.
     * @param authorities Collection of granted authorities.
     */
    public UsernameOtpAuthenticationToken(
        String username,
        String otp,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.username = username;
        this.otp = otp;
        // Token is trusted once authorities are assigned
        super.setAuthenticated(true);
    }

    /**
     * Constructs a fully authenticated token with user account details.
     *
     * @param username    The identifier of the user.
     * @param otp         The one-time code or magic link token.
     * @param account     The authenticated user account.
     * @param authorities Collection of granted authorities.
     */
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
        // Authentication successfully bound to account
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
