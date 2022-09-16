package it.smartcommunitylab.aac.internal.provider;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.ConfirmKeyAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public class InternalAuthenticationProvider
        extends ExtendedAuthenticationProvider<InternalUserAuthenticatedPrincipal, InternalUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final InternalIdentityProviderConfig config;

    private final InternalAccountProvider accountProvider;
    private final ConfirmKeyAuthenticationProvider confirmKeyProvider;

    public InternalAuthenticationProvider(String providerId,
            InternalAccountProvider accountProvider,
            InternalIdentityConfirmService confirmService,
            InternalIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountProvider, "account provider is mandatory");
        Assert.notNull(confirmService, "account confirm service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.config = providerConfig;
        this.accountProvider = accountProvider;

        // build confirm key provider
        confirmKeyProvider = new ConfirmKeyAuthenticationProvider(providerId, confirmService, realm);

    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        // just delegate to provider
        String username = authentication.getName();
        String credentials = String
                .valueOf(authentication.getCredentials());

        InternalUserAccount account = accountProvider.findAccount(username);
        if (account == null) {
            throw new InternalAuthenticationException(username, username, credentials, "unknown",
                    new BadCredentialsException("invalid user or key"));
        }

        // userId is equal to subject
        String subject = account.getUserId();

        // check whether account is locked
        if (account.isLocked()) {
            logger.debug("account is locked");
            // throw generic error to avoid account status leak
            AuthenticationException e = new BadCredentialsException("invalid request");
            throw new InternalAuthenticationException(subject, username, credentials, "password", e,
                    e.getMessage());
        }

        try {
            return confirmKeyProvider.authenticate(authentication);
        } catch (AuthenticationException e) {
            throw new InternalAuthenticationException(subject, username, credentials, "confirmKey", e,
                    e.getMessage());
        }

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return confirmKeyProvider.supports(authentication);
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

        InternalUserAuthenticatedPrincipal user = new InternalUserAuthenticatedPrincipal(getProvider(),
                getRealm(),
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
        return Instant.now().plusSeconds(config.getMaxSessionDuration());
    }
}
