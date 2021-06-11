package it.smartcommunitylab.aac.core;

import org.springframework.security.core.Authentication;

import it.smartcommunitylab.aac.core.auth.ClientAuthentication;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;

public interface AuthenticationHelper {

    public boolean isAuthenticated();

    public Authentication getAuthentication();

    /*
     * User auth
     */
    public boolean isUserAuthentication();

    public UserAuthentication getUserAuthentication();

    public UserDetails getUserDetails();

    /*
     * Client auth
     */
    public boolean isClientAuthentication();

    public ClientAuthentication getClientAuthentication();

    public ClientDetails getClientDetails();

}
