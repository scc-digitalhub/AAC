package it.smartcommunitylab.aac.core;

import org.springframework.security.core.Authentication;

import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;

public interface AuthenticationHelper {

    public Authentication getAuthentication();

    public UserAuthenticationToken getUserAuthentication();

    public UserDetails getUserDetails();

}
