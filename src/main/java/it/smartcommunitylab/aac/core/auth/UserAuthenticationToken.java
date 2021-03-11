package it.smartcommunitylab.aac.core.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.Subject;

/**
 *
 */
public class UserAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    // auth principal is the subject
    private final Subject principal;
    // subject userDetails with multiple identities bound
    private UserDetails details;
    // we collect authentications for identities
    // this way consumers will be able to verify if a identity is authenticated
    // (by default only authenticated identities should populate userDetails)
    // note: we could have more than one token for the same identity, someone else
    // should evaluate
    // we should also purge expired auth tokens
    private final Set<ExtendedAuthenticationToken> tokens;

    // web authentication details
    private WebAuthenticationDetails webAuthenticationDetails;

    public UserAuthenticationToken(
            Subject principal,
            ExtendedAuthenticationToken auth,
            UserIdentity identity,
            Collection<UserAttributes> attributeSets,
            Collection<? extends GrantedAuthority> authorities) {
        // we set authorities via super
        // we don't support null authorities list
        super(authorities);

        Assert.notEmpty(authorities, "authorities can not be empty");
        Assert.notNull(principal, "principal is required");
        Assert.notNull(auth, "auth token for identity is required");
        Assert.notNull(identity, "identity is required");

        this.principal = principal;
        this.details = new UserDetails(principal.getSubjectId(), identity, attributeSets, authorities);

        this.tokens = new HashSet<>();
        this.tokens.add(auth);
        boolean isAuthenticated = auth.isAuthenticated();

        super.setAuthenticated(isAuthenticated); // must use super, as we override
    }

    public UserAuthenticationToken(
            Subject principal, Collection<? extends GrantedAuthority> authorities,
            UserAuthenticationToken... authenticationTokens) {
        super(authorities);

        Assert.notEmpty(authorities, "authorities can not be empty");
        Assert.notNull(principal, "principal is required");
        Assert.notEmpty(authenticationTokens, "at least one authentication token is required");

        this.principal = principal;
        this.tokens = new HashSet<>();

        // use first token as base
        UserAuthenticationToken token = authenticationTokens[0];
        boolean isAuthenticated = token.isAuthenticated();
        this.details = new UserDetails(principal.getSubjectId(), token.getUser().getIdentities(),
                token.getUser().getAttributeSets(), authorities);

        // add auth tokens
        this.tokens.addAll(token.getAuthentications());

        // process additional tokens
        Arrays.stream(authenticationTokens).skip(1).forEach(t -> {
            // identities
            for (UserIdentity i : t.getUser().getIdentities()) {
                details.addIdentity(i);
            }

            // attributes
            for (UserAttributes ras : t.getUser().getAttributeSets()) {
                details.addAttributeSet(ras);
            }

            // tokens
            tokens.addAll(t.getAuthentications());
        });

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

    @JsonIgnore
    public Subject getSubject() {
        return principal;
    }

    public String getSubjectId() {
        return principal.getSubjectId();
    }

    @JsonIgnore
    public UserDetails getUser() {
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

    public void addAuthentication(ExtendedAuthenticationToken auth) {
        // TODO implement a proper lock
        synchronized (this) {
            this.tokens.add(auth);
        }
    }

    public ExtendedAuthenticationToken getAuthentication(
            String realm,
            String authority,
            String provider,
            String userId) {
        ExtendedAuthenticationToken token = null;
        for (ExtendedAuthenticationToken t : tokens) {
            if (t.getRealm().equals(realm)
                    && t.getAuthority().equals(authority)
                    && t.getProvider().equals(provider)
                    && t.getPrincipal().getUserId().equals(userId)) {
                token = t;
                break;
            }
        }

        // we return the original
        // we expect consumers to avoid mangling the token or resetting the
        // authenticated flag
        return token;
    }

    public void eraseAuthentication(ExtendedAuthenticationToken auth) {
        // TODO implement a proper lock
        synchronized (this) {
            this.tokens.remove(auth);
        }
    }

    public Set<ExtendedAuthenticationToken> getAuthentications() {
        return tokens;
    }

    /*
     * web auth details
     */
    public WebAuthenticationDetails getWebAuthenticationDetails() {
        return webAuthenticationDetails;
    }

    public void setWebAuthenticationDetails(WebAuthenticationDetails webAuthenticationDetails) {
        this.webAuthenticationDetails = webAuthenticationDetails;
    }

    @Override
    public String toString() {
        return "UserAuthenticationToken [principal=" + principal + ", details=" + details + ", tokens=" + tokens + "]";
    }

}
