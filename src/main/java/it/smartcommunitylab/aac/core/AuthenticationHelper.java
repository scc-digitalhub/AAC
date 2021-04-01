package it.smartcommunitylab.aac.core;

import org.springframework.security.core.Authentication;

import it.smartcommunitylab.aac.core.auth.ClientAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;

public interface AuthenticationHelper {

    public boolean isAuthenticated();

    public Authentication getAuthentication();

    /*
     * User auth
     */
    public boolean isUserAuthentication();

    public UserAuthenticationToken getUserAuthentication();

    public UserDetails getUserDetails();

    /*
     * Client auth
     */
    public boolean isClientAuthentication();

    public ClientAuthenticationToken getClientAuthentication();

    public ClientDetails getClientDetails();

}
