package it.smartcommunitylab.aac.core;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.model.Subject;

@Service
public class SessionManager {

    public void setSession(UserAuthenticationToken auth) {
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public void destroySession() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public void refreshSession() {
        // not needed
    }

//    public UserAuthenticationToken mergeSession(UserAuthenticationToken auth) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null) {
//            // new session
//            SecurityContextHolder.getContext().setAuthentication(auth);
//            return auth;
//        } else if (authentication instanceof AnonymousAuthenticationToken) {
//            // upgrade session
//            SecurityContextHolder.getContext().setAuthentication(auth);
//            return auth;
//        } else if (authentication instanceof UserAuthenticationToken) {
//            // try to merge
//            UserAuthenticationToken uauth = (UserAuthenticationToken) authentication;
//            Subject principal = uauth.getSubject();
//            if (principal.equals(auth.getSubject())) {
//                // same subject, add identity + attributes + token
//                // merge authorities
//                Set<GrantedAuthority> authorities = new HashSet<>();
//                authorities.addAll(uauth.getAuthorities());
//                authorities.addAll(auth.getAuthorities());
//
//                // current authentication is first, new extends
//                UserAuthenticationToken nauth = new UserAuthenticationToken(principal, authorities, uauth, auth);
//
//                // set as active
//                SecurityContextHolder.getContext().setAuthentication(nauth);
//                return nauth;
//            } else {
//                // replace, TODO multi subject sessions with switcher
//                SecurityContextHolder.getContext().setAuthentication(auth);
//                return auth;
//            }
//        } else {
//            // can't handle
//            return null;
//        }
//
//    }

    /*
     * Authentication
     */

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public UserAuthenticationToken getUserAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UserAuthenticationToken) {
            return (UserAuthenticationToken) auth;
        } else {
            return null;
        }

    }

    public UserDetails getUserDetails() {
        UserAuthenticationToken auth = getUserAuthentication();
        if (auth == null) {
            return null;
        }

        return auth.getUser();
    }

    // TODO track active sessions via SessionRegistry

    public void listSession() {

    }

    public void listSessions(String providerId) {

    }

    public void destroySessions(String providerId) {
        // destroy sessions for users with an authentication from the given provider
        // we revoke session but not tokens
    }

}
