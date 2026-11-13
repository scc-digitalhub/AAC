package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.otp.auth.UsernameOtpAuthenticationProvider;
import it.smartcommunitylab.aac.otp.auth.UsernameOtpAuthenticationToken;
import it.smartcommunitylab.aac.otp.model.InternalOtpUserAuthenticatedPrincipal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

public class OtpAuthenticationProvider
    extends ExtendedAuthenticationProvider<InternalOtpUserAuthenticatedPrincipal, InternalUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String ACCOUNT_NOT_FOUND_OTP = "internalAccountNotFoundOtp";

    private final OtpIdentityProviderConfig config;
    private final String repositoryId;
    private final UserAccountService<InternalUserAccount> userAccountService;
    private final UsernameOtpAuthenticationProvider authProvider;

    private final String userNotFoundEncodedOtp;
    private final PasswordEncoder otpEncoder;

    public OtpAuthenticationProvider(
        String providerId,
        UserAccountService<InternalUserAccount> userAccountService,
        OtpIdentityCredentialsService otpService,
        OtpIdentityProviderConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_OTP, providerId, realm);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(otpService, "otp service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.config = providerConfig;
        this.repositoryId = config.getRepositoryId();
        this.userAccountService = userAccountService;
        this.otpEncoder = new InternalPasswordEncoder();

        this.userNotFoundEncodedOtp = this.otpEncoder.encode(ACCOUNT_NOT_FOUND_OTP);

        this.authProvider = new UsernameOtpAuthenticationProvider(providerId, userAccountService, repositoryId, realm);
    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String credentials = String.valueOf(authentication.getCredentials());

        List<InternalUserAccount> accounts = userAccountService.findAccountsByUser(this.repositoryId, username);

        if (accounts.isEmpty()) {
            verifyDummyOtpIfApplicable(authentication);
            throw buildAuthException(
                "unknown",
                username,
                credentials,
                new BadCredentialsException("invalid user or otp")
            );
        }

        InternalUserAccount account = accounts.get(0);
        String subject = account.getUserId();

        if (config.isRequireAccountConfirmation() && !account.isConfirmed()) {
            logger.debug("account is not verified and confirmation is required to login");
            throw buildAuthException(
                "otp",
                username,
                credentials,
                subject,
                new BadCredentialsException("invalid request")
            );
        }

        if (account.isLocked()) {
            logger.debug("account is locked");
            throw buildAuthException(
                "otp",
                username,
                credentials,
                subject,
                new BadCredentialsException("invalid request")
            );
        }

        if (authentication instanceof UsernameOtpAuthenticationToken authRequest) {
            try {
                UsernameOtpAuthenticationToken authToProcess = new UsernameOtpAuthenticationToken(
                    account.getUsername(),
                    authRequest.getOtp(),
                    authRequest.getAuthorities()
                );
                authToProcess.setDetails(authentication.getDetails());
                return authProvider.authenticate(authToProcess);
            } catch (AuthenticationException e) {
                throw buildAuthException("otp", username, credentials, subject, e);
            }
        }

        throw buildAuthException(
            "unknown",
            username,
            credentials,
            subject,
            new BadCredentialsException("invalid request")
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernameOtpAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected InternalOtpUserAuthenticatedPrincipal createUserPrincipal(Object account) {
        if (account == null) {
            return null;
        }
        if (account instanceof InternalUserAccount userAccount) {
            return new InternalOtpUserAuthenticatedPrincipal(userAccount);
        }
        throw new IllegalArgumentException("Account object is not of type InternalUserAccount");
    }

    private void verifyDummyOtpIfApplicable(Authentication authentication) {
        if (authentication instanceof UsernameOtpAuthenticationToken auth && auth.getCredentials() != null) {
            this.otpEncoder.matches(auth.getOtp(), this.userNotFoundEncodedOtp);
        }
    }

    private InternalAuthenticationException buildAuthException(
        String errorType,
        String username,
        String credentials,
        AuthenticationException cause
    ) {
        return buildAuthException(errorType, username, credentials, username, cause);
    }

    private InternalAuthenticationException buildAuthException(
        String errorType,
        String username,
        String credentials,
        String subject,
        AuthenticationException cause
    ) {
        return new InternalAuthenticationException(
            subject,
            username,
            credentials,
            errorType,
            cause,
            cause.getMessage()
        );
    }
}
