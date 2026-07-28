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

    // provider configuration
    private final OtpIdentityProviderConfig config;
    private final String repositoryId;

    private final UserAccountService<InternalUserAccount> userAccountService;
    private final UsernameOtpAuthenticationProvider authProvider;

    private volatile String userNotFoundEncodedOtp;
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

        authProvider = new UsernameOtpAuthenticationProvider(providerId, userAccountService, repositoryId, realm);
    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        if (this.userNotFoundEncodedOtp == null) {
            this.userNotFoundEncodedOtp = this.otpEncoder.encode(ACCOUNT_NOT_FOUND_OTP);
        }

        String username = authentication.getName();
        String credentials = String.valueOf(authentication.getCredentials());

        List<InternalUserAccount> accounts = userAccountService.findAccountsByUser(this.repositoryId, username);

        if (accounts.isEmpty()) {
            if (authentication instanceof UsernameOtpAuthenticationToken && authentication.getCredentials() != null) {
                String otp = ((UsernameOtpAuthenticationToken) authentication).getOtp();
                this.otpEncoder.matches(otp, this.userNotFoundEncodedOtp);
            }

            throw new InternalAuthenticationException(
                username,
                username,
                credentials,
                "unknown",
                new BadCredentialsException("invalid user or otp")
            );
        }

        InternalUserAccount account = accounts.get(0);
        String subject = account.getUserId();

        // check whether confirmation is required and user is confirmed
        if (config.isRequireAccountConfirmation() && !account.isConfirmed()) {
            logger.debug("account is not verified and confirmation is required to login");

            // throw generic error to avoid account status leak
            AuthenticationException e = new BadCredentialsException("invalid request");
            throw new InternalAuthenticationException(subject, username, credentials, "otp", e, e.getMessage());
        }

        // check whether account is locked
        if (account.isLocked()) {
            logger.debug("account is locked");

            // throw generic error to avoid account status leak
            AuthenticationException e = new BadCredentialsException("invalid request");
            throw new InternalAuthenticationException(subject, username, credentials, "otp", e, e.getMessage());
        }

        if (authentication instanceof UsernameOtpAuthenticationToken) {
            try {
                UsernameOtpAuthenticationToken authRequest = (UsernameOtpAuthenticationToken) authentication;
                UsernameOtpAuthenticationToken authToProcess = new UsernameOtpAuthenticationToken(
                    account.getUsername(),
                    authRequest.getOtp(),
                    authRequest.getAuthorities()
                );
                authToProcess.setDetails(authentication.getDetails());
                return authProvider.authenticate(authToProcess);
            } catch (AuthenticationException e) {
                throw new InternalAuthenticationException(subject, username, credentials, "otp", e, e.getMessage());
            }
        }

        throw new InternalAuthenticationException(
            subject,
            username,
            credentials,
            "unknown",
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
        if (account instanceof InternalUserAccount) {
            return new InternalOtpUserAuthenticatedPrincipal((InternalUserAccount) account);
        }
        throw new IllegalArgumentException("Account object is not of type InternalUserAccount");
    }
}
