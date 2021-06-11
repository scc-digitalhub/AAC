package it.smartcommunitylab.aac.core.auth;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.model.Subject;

/**
 *
 */
public abstract class UserAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    // auth principal is the subject
    protected final Subject principal;

    // the token is bound to a single realm, since we can match identities only over
    // same realm by design
    protected final String realm;

    public UserAuthentication(
            Subject principal, String realm,
            Collection<? extends GrantedAuthority> authorities,
            boolean isAuthenticated) {
        // we set authorities via super
        // we don't support null authorities list
        super(authorities);

        Assert.notEmpty(authorities, "authorities can not be empty");
        Assert.notNull(principal, "principal is required");
        Assert.notNull(realm, "realm is required");

        this.principal = principal;
        this.realm = realm;

        super.setAuthenticated(isAuthenticated); // must use super, as we override
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private UserAuthentication() {
        this(null, null, null, false);
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
    public String getName() {
        return principal.getSubjectId();
    }

    @Override
    public abstract Object getDetails();

    @JsonIgnore
    public Subject getSubject() {
        return principal;
    }

    public String getRealm() {
        return realm;
    }

    public String getSubjectId() {
        return principal.getSubjectId();
    }

    @JsonIgnore
    public abstract UserDetails getUser();

    @Override
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

    public abstract ExtendedAuthenticationToken getAuthentication(
            String authority,
            String provider,
            String userId);

    public abstract void eraseAuthentication(ExtendedAuthenticationToken auth);

    public abstract Set<ExtendedAuthenticationToken> getAuthentications();

    /*
     * web auth details
     */
    public abstract WebAuthenticationDetails getWebAuthenticationDetails();

}
