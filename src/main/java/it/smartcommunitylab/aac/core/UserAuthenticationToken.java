package it.smartcommunitylab.aac.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.model.UserIdentity;

/**
 * @author raman
 *
 */
public class UserAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    private final Subject principal;
    // subject user with multiple identities bound
    private User details;
    // we collect authentications for identities
    // this way consumers will be able to check if a identity is authenticated
    private Set<RealmAuthenticationToken> tokens;

    public UserAuthenticationToken(
            Subject principal,
            RealmAuthenticationToken auth,
            UserIdentity identity, Collection<? extends GrantedAuthority> authorities) {
        // we set authorities via super
        // we support null authorities list
        super(authorities);

        Assert.notNull(principal, "principal is required");
        Assert.notNull(auth, "auth token for identity is required");
        Assert.notNull(identity, "identity is required");

        this.principal = principal;
        this.details = new User(principal.getSubject(), identity, authorities);

        this.tokens = new HashSet<>();
        this.tokens.add(auth);
        boolean isAuthenticated = auth.isAuthenticated();

        super.setAuthenticated(isAuthenticated); // must use super, as we override
    }

    @Override
    public Object getCredentials() {
        // no credentials here, we expect those handled at account level
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getDetails() {
        return details;
    }

    public String getSubject() {
        return principal.getSubject();
    }

    public User getUser() {
        return details;
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }

        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
    }

    /*
     * Auth tokens
     */

    public void addAuthentication(RealmAuthenticationToken auth) {
        // TODO implement a proper lock
        synchronized (this) {
            this.tokens.add(auth);
        }
    }

    public RealmAuthenticationToken getAuthentication(String realm, String authority, String provider,
            String principal) {
        RealmAuthenticationToken token = null;
        for (RealmAuthenticationToken t : tokens) {
            if (t.getRealm().equals(realm)
                    && t.getAuthority().equals(authority)
                    && t.getProvider().equals(provider)
                    && t.getPrincipal().equals(principal)) {
                token = t;
                break;
            }
        }

        return token;
    }

    public void eraseAuthentication(RealmAuthenticationToken auth) {
        // TODO implement a proper lock
        synchronized (this) {
            this.tokens.remove(auth);
        }
    }

    @Override
    public String toString() {
        return "UserAuthenticationToken [principal=" + principal + ", details=" + details + ", tokens=" + tokens + "]";
    }

}
