package it.smartcommunitylab.aac.core.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.SecurityContextAccessor;

import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;

public class DefaultSecurityContextAuthenticationHelper implements AuthenticationHelper, SecurityContextAccessor {

    @Override
    public boolean isAuthenticated() {
        return (getAuthentication() != null);
    }

    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /*
     * Authentication: user
     */

    @Override
    public boolean isUserAuthentication() {
        return (getUserAuthentication() != null);
    }

    @Override
    public UserAuthenticationToken getUserAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UserAuthenticationToken) {
            return (UserAuthenticationToken) auth;
        }
        if (auth instanceof ComposedAuthenticationToken) {
            return ((ComposedAuthenticationToken) auth).getUserAuthentication();
        } else {
            return null;
        }

    }

    @Override
    public UserDetails getUserDetails() {
        UserAuthenticationToken auth = getUserAuthentication();
        if (auth == null) {
            return null;
        }

        return auth.getUser();
    }

    @Override
    public boolean isUser() {
        UserAuthenticationToken auth = getUserAuthentication();
        return auth != null;
    }

    /*
     * Authentication: client
     */

    @Override
    public boolean isClientAuthentication() {
        return (getUserAuthentication() != null);

    }

    @Override
    public ClientAuthenticationToken getClientAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ClientAuthenticationToken) {
            return (ClientAuthenticationToken) auth;
        } else if (auth instanceof ComposedAuthenticationToken) {
            return ((ComposedAuthenticationToken) auth).getClientAuthentication();
        } else {
            return null;
        }

    }

    @Override
    public ClientDetails getClientDetails() {
        ClientAuthenticationToken auth = getClientAuthentication();
        if (auth == null) {
            return null;
        }

        return auth.getClient();
    }

    /*
     * Authorities
     */

    @Override
    public Set<GrantedAuthority> getAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<GrantedAuthority>(authentication.getAuthorities()));
    }

}
