package it.smartcommunitylab.aac.internal.provider;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.DefaultUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.internal.auth.ConfirmKeyAuthenticationToken;
import it.smartcommunitylab.aac.internal.auth.ConfirmKeyAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.ResetKeyAuthenticationProvider;
import it.smartcommunitylab.aac.internal.auth.ResetKeyAuthenticationToken;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.internal.service.InternalUserDetailsService;

public class InternalAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalUserDetailsService userDetailsService;
    private final DaoAuthenticationProvider authProvider;
    private final ConfirmKeyAuthenticationProvider confirmKeyProvider;
    private final ResetKeyAuthenticationProvider resetKeyProvider;

    public InternalAuthenticationProvider(String providerId,
            InternalUserAccountService userAccountService,
            InternalAccountService accountService, InternalPasswordService passwordService,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");

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
            return authProvider.authenticate(authentication);
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
        String username = null;

        if (principal instanceof UserDetails) {
            // we need to unpack user and fetch properties from repo
            UserDetails details = (UserDetails) principal;
            username = details.getUsername();
        } else {
            // assume a string
            username = (String) principal;
        }

        // TODO complete mapping, for now this suffices
        String userId = this.exportInternalId(username);

        // fallback to username
        String name = username;
        // add auth-related attributes
        Map<String, String> attributes = new HashMap<>();

        Instant now = new Date().toInstant();
        // format date
        attributes.put("loginDate", DateTimeFormatter.ISO_INSTANT.format(now));

        DefaultUserAuthenticatedPrincipal user = new DefaultUserAuthenticatedPrincipal(SystemKeys.AUTHORITY_INTERNAL,
                getProvider(), getRealm(), userId);
        user.setName(name);
        user.setAttributes(attributes);

        return user;
    }

}
