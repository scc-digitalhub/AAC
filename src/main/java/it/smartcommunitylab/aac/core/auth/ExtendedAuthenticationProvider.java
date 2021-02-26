package it.smartcommunitylab.aac.core.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;

public abstract class ExtendedAuthenticationProvider extends AbstractConfigurableProvider
        implements AuthenticationProvider {

    public ExtendedAuthenticationProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }
    
    @Override
    public String getType() {
        return SystemKeys.RESOURCE_AUTHENTICATION;
    }

    public ExtendedAuthenticationToken authenticateExtended(Authentication authentication)
            throws AuthenticationException {

        // process authentication
        // subclasses should implement the validation
        // TODO implement preAuthChecks
        Authentication authResponse = authenticate(authentication);

        // TODO implement postAuthChecks

        // we expect a fully populated user principal
        UserAuthenticatedPrincipal principal = createUserPrincipal(authResponse.getPrincipal());

        // create the ext token
        // subclasses could re-implement the method

        return createExtendedAuthentication(principal, authResponse);

    }

    // subclasses need to implement this, they should have the knowledge
    protected abstract UserAuthenticatedPrincipal createUserPrincipal(Object principal);

    protected ExtendedAuthenticationToken createExtendedAuthentication(UserAuthenticatedPrincipal principal,
            Authentication authentication) {
        // build the token with both the extracted userPrincipal and the original auth
        // note that the token could contain the original credentials
        // we want those for further processing, will be cleared later by manager
        return new ExtendedAuthenticationToken(
                getAuthority(), getProvider(), getRealm(),
                principal,
                authentication);
    }

}
