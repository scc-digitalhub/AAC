package it.smartcommunitylab.aac.core;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SessionManager implements AuthenticationHelper {

    public void setSession(UserAuthenticationToken auth) {
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public void destroySession() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public void refreshSession() {
        // not needed
    }

    /*
     * Authentication
     */

    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public UserAuthenticationToken getUserAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UserAuthenticationToken) {
            return (UserAuthenticationToken) auth;
        } else {
            return null;
        }

    }

    @Override
    public User getUserDetails() {
        UserAuthenticationToken auth = getUserAuthentication();
        if (auth == null) {
            return null;
        }

        return auth.getUser();
    }

    // TODO track active sessions via SessionRegistry

}
