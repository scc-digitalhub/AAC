package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.core.auth.ClientAuthentication;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import org.springframework.security.core.Authentication;

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
