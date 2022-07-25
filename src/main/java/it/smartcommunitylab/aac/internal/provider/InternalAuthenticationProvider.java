package it.smartcommunitylab.aac.internal.provider;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.internal.auth.ConfirmKeyAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.ConfirmKeyAuthenticationToken;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.auth.ResetKeyAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.ResetKeyAuthenticationToken;
import it.smartcommunitylab.aac.internal.auth.UsernamePasswordAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.UsernamePasswordAuthenticationToken;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;

public class InternalAuthenticationProvider
        extends ExtendedAuthenticationProvider<InternalUserAuthenticatedPrincipal, InternalUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String ACCOUNT_NOT_FOUND_PASSWORD = "internalAccountNotFoundPassword";

    // provider configuration
    private final InternalIdentityProviderConfig config;
    private final String repositoryId;

    private final InternalUserAccountService userAccountService;
    private final UsernamePasswordAuthenticationProvider authProvider;
    private final ConfirmKeyAuthenticationProvider confirmKeyProvider;
    private final ResetKeyAuthenticationProvider resetKeyProvider;

    // use a volatile password to mitigate timing attacks: ensures encoder is always
    // run
    private volatile String userNotFoundEncodedPassword;
    private final PasswordEncoder passwordEncoder;

    public InternalAuthenticationProvider(String providerId,
            InternalUserAccountService userAccountService,
            InternalAccountService accountService, InternalPasswordService passwordService,
            InternalIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        this.config = providerConfig;
        this.userAccountService = userAccountService;

        this.repositoryId = config.getRepositoryId();

        // build a password encoder
        this.passwordEncoder = new InternalPasswordEncoder();

        // build our internal auth provider
        authProvider = new UsernamePasswordAuthenticationProvider(providerId, accountService, passwordService, realm);
//        // we use our password encoder
//        authProvider.setPasswordEncoder(passwordEncoder);

        // build additional providers
        // TODO check config to see if these are available
        confirmKeyProvider = new ConfirmKeyAuthenticationProvider(providerId, accountService, realm);
        resetKeyProvider = new ResetKeyAuthenticationProvider(providerId, accountService, passwordService,
                realm);

    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        // mitigate timing attacks by encoding the notFound password
        // performed for all providers
        if (this.userNotFoundEncodedPassword == null) {
            this.userNotFoundEncodedPassword = this.passwordEncoder.encode(ACCOUNT_NOT_FOUND_PASSWORD);
        }

        // just delegate to provider
        String username = authentication.getName();
        String credentials = String
                .valueOf(authentication.getCredentials());

        InternalUserAccount account = userAccountService.findAccountByUsername(repositoryId, username);
        if (account == null) {
            // mitigate timing attacks to encode the provider password if usernamePassword
            if (authentication instanceof UsernamePasswordAuthenticationToken
                    && authentication.getCredentials() != null) {
                String password = ((UsernamePasswordAuthenticationToken) authentication).getPassword();
                this.passwordEncoder.matches(password, this.userNotFoundEncodedPassword);
            }

            throw new InternalAuthenticationException(username, username, credentials, "unknown",
                    new BadCredentialsException("invalid user or password"));
        }

        // userId is equal to subject
        String subject = account.getUserId();

        // check whether confirmation is required and user is confirmed
        if (!(authentication instanceof ConfirmKeyAuthenticationToken) && config.isConfirmationRequired()
                && !account.isConfirmed()) {
            logger.debug("account is not verified and confirmation is required to login");
            // throw generic error to avoid account status leak
            AuthenticationException e = new BadCredentialsException("invalid request");
            throw new InternalAuthenticationException(subject, username, credentials, "password", e,
                    e.getMessage());
        }

        // check whether account is locked
        if (account.isLocked()) {
            logger.debug("account is locked");
            // throw generic error to avoid account status leak
            AuthenticationException e = new BadCredentialsException("invalid request");
            throw new InternalAuthenticationException(subject, username, credentials, "password", e,
                    e.getMessage());
        }

        // TODO check if providers are available
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            try {
                return authProvider.authenticate(authentication);
            } catch (AuthenticationException e) {
                throw new InternalAuthenticationException(subject, username, credentials, "password", e,
                        e.getMessage());
            }
        } else if (authentication instanceof ConfirmKeyAuthenticationToken) {
            try {
                return confirmKeyProvider.authenticate(authentication);
            } catch (AuthenticationException e) {
                throw new InternalAuthenticationException(subject, username, credentials, "confirmKey", e,
                        e.getMessage());
            }
        } else if (authentication instanceof ResetKeyAuthenticationToken) {
            try {
                return resetKeyProvider.authenticate(authentication);
            } catch (AuthenticationException e) {
                throw new InternalAuthenticationException(subject, username, credentials, "resetKey", e,
                        e.getMessage());
            }
        }
        throw new InternalAuthenticationException(subject, username, credentials, "unknown",
                new BadCredentialsException("invalid request"));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (authProvider.supports(authentication)
                || confirmKeyProvider.supports(authentication)
                || resetKeyProvider.supports(authentication));
    }

    @Override
    protected InternalUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        InternalUserAccount account = (InternalUserAccount) principal;
        String userId = account.getUserId();
        String username = account.getUsername();
//        StringBuilder fullName = new StringBuilder();
//        fullName.append(account.getName()).append(" ").append(account.getSurname());
//
//        String name = fullName.toString();
//        if (!StringUtils.hasText(name)) {
//            name = username;
//        }

        InternalUserAuthenticatedPrincipal user = new InternalUserAuthenticatedPrincipal(getProvider(), getRealm(),
                userId, username);
        // set principal name as username
        user.setName(username);
        // set attributes to support mapping in idp
        user.setAccountAttributes(account);

        return user;
    }

    @Override
    protected Instant expiresAt(Authentication auth) {
        // build expiration with maxAge
        return Instant.now().plusSeconds(config.getConfigMap().getMaxSessionDuration());
    }
}
