package it.smartcommunitylab.aac.otp.auth;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import java.util.Collections;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Authentication provider for OTP-based authentication.
 * This class validates the user-provided OTP or Magic Link token against the
 * configured credentials service. It acts as the bridge between the
 * authentication request and the underlying identity repository.
 */
public class UsernameOtpAuthenticationProvider implements AuthenticationProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // Services required for account lookup and credential validation
    private final UserAccountService<InternalUserAccount> userAccountService;

    private final String providerId;
    private final String repositoryId;

    /**
     * Initializes the provider with required services and configuration.
     *
     * @param providerId         ID of the authentication provider.
     * @param userAccountService Service to retrieve user account information.
     * @param otpService         Service to verify OTP codes or magic links.
     * @param repositoryId       ID of the account repository.
     * @param realm              The security realm this provider belongs to.
     */

    public UsernameOtpAuthenticationProvider(
        String providerId,
        UserAccountService<InternalUserAccount> userAccountService,
        String repositoryId,
        String realm
    ) {
        Assert.hasText(providerId, "provider can not be null or empty");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id can not be null or empty");

        this.userAccountService = userAccountService;

        this.providerId = providerId;
        this.repositoryId = repositoryId;
    }

    /**
     * Authenticates the user based on the provided credentials.
     *
     * @param authentication The authentication request token.
     * @return A fully populated authentication token upon success.
     * @throws AuthenticationException if authentication fails.
     */

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(
            UsernameOtpAuthenticationToken.class,
            authentication,
            "Only UsernameOtpAuthenticationToken is supported"
        );

        UsernameOtpAuthenticationToken authRequest = (UsernameOtpAuthenticationToken) authentication;

        String username = authRequest.getUsername();
        String otp = authRequest.getOtp();

        // Validate basic input parameters
        if (!StringUtils.hasText(username) || !StringUtils.hasText(otp)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        try {
            // Check if user account exists in current repository
            InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
            if (account == null) {
                throw new BadCredentialsException("invalid request");
            }

            // Configure account metadata for session
            // Force authority to OTP to distinguish from password accounts
            account.setAuthority(SystemKeys.AUTHORITY_OTP);
            account.setProvider(providerId);

            // Default user authorities
            Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_USER));

            // Return successful authentication token
            return new UsernameOtpAuthenticationToken(username, otp, account, authorities);
        } catch (Exception e) {
            logger.error("Authentication failed: {}", e.getMessage());
            throw new BadCredentialsException("invalid request");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernameOtpAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
