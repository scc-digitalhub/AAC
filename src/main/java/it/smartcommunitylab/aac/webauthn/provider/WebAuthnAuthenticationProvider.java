package it.smartcommunitylab.aac.webauthn.provider;

import java.time.Instant;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.auth.RegisterCredentialAuthenticationToken;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserDetailsService;

public class WebAuthnAuthenticationProvider extends ExtendedAuthenticationProvider {

    private final WebAuthnIdentityProviderConfigMap config;

    private final WebAuthnUserAccountService userAccountService;
    private final WebAuthnUserDetailsService userDetailsService;
    private final DaoAuthenticationProvider authProvider;

    public WebAuthnAuthenticationProvider(String providerId,
            WebAuthnUserAccountService userAccountService,
            WebAuthnAccountService accountService,
            WebAuthnCredentialsService credentialsService,
            WebAuthnIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        this.config = providerConfig.getConfigMap();
        this.userAccountService = userAccountService;

        // build a userDetails service
        userDetailsService = new WebAuthnUserDetailsService(userAccountService, realm);

        // build our WebAuthn auth provider by wrapping spring dao authprovider
        authProvider = new DaoAuthenticationProvider();
        // set user details service
        authProvider.setUserDetailsService(this.userDetailsService);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authProvider.supports(authentication);
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication) {
        String username = authentication.getName();

        WebAuthnUserAccount account = null;
        try {
            account = userAccountService
                    .findByProviderAndUsername(getProvider(), username);
        } catch (NoSuchUserException _e) {
        }
        if (account == null) {
            throw new WebAuthnAuthenticationException(
                    "authentication failed");
        }
        // String subject = account.getSubject();
        if (authentication instanceof RegisterCredentialAuthenticationToken) {

            try {
                RegisterCredentialAuthenticationToken token = (RegisterCredentialAuthenticationToken) authProvider
                        .authenticate(authentication);
                if (token == null) {
                    return null;
                }

                // rebuild token to include account
                username = token.getName();

                RegisterCredentialAuthenticationToken auth = new RegisterCredentialAuthenticationToken(username,
                        account,
                        token.getAuthorities());
                auth.setDetails(token.getDetails());
                return auth;
            } catch (AuthenticationException e) {
                throw new WebAuthnAuthenticationException(e.getMessage());
            }
        }
        throw new WebAuthnAuthenticationException(
                "authentication failed");
    }

    @Override
    protected UserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        WebAuthnUserAccount account = (WebAuthnUserAccount) principal;
        String username = account.getUsername();
        String name = account.getUsername();

        WebAuthnUserAuthenticatedPrincipal user = new WebAuthnUserAuthenticatedPrincipal(getProvider(), getRealm(),
                exportInternalId(username));
        user.setName(name);
        user.setPrincipal(account);

        return user;
    }

    @Override
    protected Instant expiresAt(Authentication auth) {
        // build expiration with maxAge
        return Instant.now().plusSeconds(config.getMaxSessionDuration());
    }

}
