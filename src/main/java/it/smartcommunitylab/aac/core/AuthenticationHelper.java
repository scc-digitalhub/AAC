package it.smartcommunitylab.aac.core;

import org.springframework.security.core.Authentication;

public interface AuthenticationHelper {

    public Authentication getAuthentication();

    public UserAuthenticationToken getUserAuthentication();

    public User getUserDetails();

}
