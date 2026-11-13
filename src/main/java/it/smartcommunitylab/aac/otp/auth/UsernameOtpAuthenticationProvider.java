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

public class UsernameOtpAuthenticationProvider implements AuthenticationProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<InternalUserAccount> userAccountService;

    private final String providerId;
    private final String repositoryId;

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

        if (!StringUtils.hasText(username) || !StringUtils.hasText(otp)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        try {
            InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
            if (account == null) {
                throw new BadCredentialsException("invalid request");
            }

            account.setAuthority(SystemKeys.AUTHORITY_OTP);
            account.setProvider(providerId);

            Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_USER));

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
