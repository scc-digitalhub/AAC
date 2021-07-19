package it.smartcommunitylab.aac.internal.provider;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.internal.auth.ConfirmKeyAuthenticationToken;
import it.smartcommunitylab.aac.internal.auth.ConfirmKeyAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.ResetKeyAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.ResetKeyAuthenticationToken;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.internal.service.InternalUserDetailsService;

public class InternalAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final InternalIdentityProviderConfigMap config;

    private final InternalUserAccountService userAccountService;
    private final InternalUserDetailsService userDetailsService;
    private final DaoAuthenticationProvider authProvider;
    private final ConfirmKeyAuthenticationProvider confirmKeyProvider;
    private final ResetKeyAuthenticationProvider resetKeyProvider;

    public InternalAuthenticationProvider(String providerId,
            InternalUserAccountService userAccountService,
            InternalAccountService accountService, InternalPasswordService passwordService,
            String realm, InternalIdentityProviderConfigMap configMap) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(configMap, "config map is mandatory");
        this.config = configMap;
        this.userAccountService = userAccountService;
        // build a userDetails service
        userDetailsService = new InternalUserDetailsService(userAccountService, realm);

        // build our internal auth provider by wrapping spring dao authprovider
        authProvider = new DaoAuthenticationProvider();
        // set user details service
        authProvider.setUserDetailsService(this.userDetailsService);
        // we use our password encoder
        authProvider.setPasswordEncoder(new InternalPasswordEncoder());

        // build additional providers
        // TODO check config to see if these are available
        confirmKeyProvider = new ConfirmKeyAuthenticationProvider(providerId, accountService, realm);
        resetKeyProvider = new ResetKeyAuthenticationProvider(providerId, accountService, passwordService,
                realm);

    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        // just delegate to provider
        // TODO check if providers are available
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authProvider
                    .authenticate(authentication);
            if (token == null) {
                return null;
            }

            // rebuild token to include account
            String username = token.getName();
            InternalUserAccount account = userAccountService.findAccountByUsername(getRealm(), username);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(account,
                    token.getCredentials(), token.getAuthorities());
            auth.setDetails(token.getDetails());
            return auth;

        } else if (authentication instanceof ConfirmKeyAuthenticationToken) {
            return confirmKeyProvider.authenticate(authentication);
        } else if (authentication instanceof ResetKeyAuthenticationToken) {
            return resetKeyProvider.authenticate(authentication);
        }

        throw new BadCredentialsException("invalid request");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (authProvider.supports(authentication)
                || confirmKeyProvider.supports(authentication)
                || resetKeyProvider.supports(authentication));
    }

    @Override
    protected UserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        InternalUserAccount account = (InternalUserAccount) principal;
        String username = account.getUsername();
        String name = account.getUsername();
//        StringBuilder fullName = new StringBuilder();
//        fullName.append(account.getName()).append(" ").append(account.getSurname());
//
//        String name = fullName.toString();
//        if (!StringUtils.hasText(name)) {
//            name = username;
//        }

        InternalUserAuthenticatedPrincipal user = new InternalUserAuthenticatedPrincipal(getProvider(), getRealm(),
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
