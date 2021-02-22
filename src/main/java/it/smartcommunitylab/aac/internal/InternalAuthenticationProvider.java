package it.smartcommunitylab.aac.internal;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.RealmAwareUserDetailsService;
import it.smartcommunitylab.aac.core.RealmAwareUsernamePasswordAuthenticationToken;

public class InternalAuthenticationProvider implements AuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // we load user from here, could be another repo
    private RealmAwareUserDetailsService userDetailsService;

    /**
     * The plaintext password used to perform PasswordEncoder#matches(CharSequence,
     * String)} on when the user is not found to avoid SEC-2056.
     */
    private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";

    // ~ Instance fields
    // ================================================================================================

    private PasswordEncoder passwordEncoder;

    /**
     * The password used to perform
     * {@link PasswordEncoder#matches(CharSequence, String)} on when the user is not
     * found to avoid SEC-2056. This is necessary, because some
     * {@link PasswordEncoder} implementations will short circuit if the password is
     * not in a valid format.
     */
    private volatile String userNotFoundEncodedPassword;

    public InternalAuthenticationProvider(RealmAwareUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // we support no realm login, we map those users to global space
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                "Only UsernamePasswordAuthenticationToken is supported");

        String realm = null;
        if (RealmAwareUsernamePasswordAuthenticationToken.class.isInstance(authentication)) {
            realm = ((RealmAwareUsernamePasswordAuthenticationToken) authentication).getRealm();
        }

        String username = authentication.getName();

        UserDetails userDetails;
        try {
            userDetails = retrieveUser(realm, username,
                    (UsernamePasswordAuthenticationToken) authentication);
        } catch (UsernameNotFoundException notFound) {
            logger.debug("User '" + username + "' not found");
            throw new BadCredentialsException("Bad credentials");
        }

        Assert.notNull(userDetails,
                "retrieveUser returned null - a violation of the interface contract");

        // check password
        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");
            throw new BadCredentialsException("Bad credentials");
        }

        String presentedPassword = authentication.getCredentials().toString();

        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            logger.debug("Authentication failed: password does not match stored value");
            throw new BadCredentialsException("Bad credentials");
        }

        // validate account status
        if (!userDetails.isAccountNonLocked()) {
            logger.debug("Authentication failed: account locked");
            throw new BadCredentialsException("Bad credentials");
        }

        // note auth token will contain credentials, someone will need to erase it
        return createSuccessAuthentication(realm, username, userDetails, authentication, userDetails.getAuthorities());

    }

    protected Authentication createSuccessAuthentication(String realm, Object principal, UserDetails details,
            Authentication authentication, Collection<? extends GrantedAuthority> authorities) {
        // Ensure we return the original credentials the user supplied,
        // so subsequent attempts are successful even with encoded passwords.
        RealmAwareUsernamePasswordAuthenticationToken result = new RealmAwareUsernamePasswordAuthenticationToken(
                realm,
                principal, authentication.getCredentials(),
                authorities);
        result.setDetails(details);

        return result;
    }

    protected void doAfterPropertiesSet() {
        Assert.notNull(this.userDetailsService, "A UserDetailsService must be set");
    }

    protected final UserDetails retrieveUser(String realm, String username,
            UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        prepareTimingAttackProtection();
        try {
            // we need userId
            UserDetails internalUser = userDetailsService.loadUserByUsername(realm, username);
            if (internalUser == null) {
                throw new InternalAuthenticationServiceException(
                        "UserDetailsService returned null, which is an interface contract violation");
            }
            return internalUser;
        } catch (UsernameNotFoundException ex) {
            mitigateAgainstTimingAttack(authentication);
            throw ex;
        } catch (InternalAuthenticationServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // we support requests with no realm, which will be mapped to null realm
        return (UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication));
    }

    /**
     * Sets the PasswordEncoder instance to be used to encode and validate
     * passwords. If not set, the password will be compared using
     * {@link PasswordEncoderFactories#createDelegatingPasswordEncoder()}
     *
     * @param passwordEncoder must be an instance of one of the
     *                        {@code PasswordEncoder} types.
     */
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        this.passwordEncoder = passwordEncoder;
        this.userNotFoundEncodedPassword = null;
    }

    protected PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public void setUserDetailsService(InternalUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    protected UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    private void prepareTimingAttackProtection() {
        if (this.userNotFoundEncodedPassword == null) {
            this.userNotFoundEncodedPassword = this.passwordEncoder.encode(USER_NOT_FOUND_PASSWORD);
        }
    }

    private void mitigateAgainstTimingAttack(UsernamePasswordAuthenticationToken authentication) {
        if (authentication.getCredentials() != null) {
            String presentedPassword = authentication.getCredentials().toString();
            this.passwordEncoder.matches(presentedPassword, this.userNotFoundEncodedPassword);
        }
    }

}
