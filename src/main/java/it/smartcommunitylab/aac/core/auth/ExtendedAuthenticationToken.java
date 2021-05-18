package it.smartcommunitylab.aac.core.auth;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;


/*
 * An authenticationToken holding both the provider token and a resolved identity
 * 
 */

public class ExtendedAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = -1302725087208017064L;

    private final UserAuthenticatedPrincipal principal;

    private final String authority;
    private final String provider;
    private final String realm;

    private Authentication token;

    // audit
    private final Date issueTime;
    // TODO add expire date and validity checks

    public ExtendedAuthenticationToken(String authority, String provider, String realm,
            UserAuthenticatedPrincipal principal,
            Authentication token) {
        super(Collections.emptyList());
        this.token = token;
        this.principal = principal;
        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
        // set creation time
        this.issueTime = Calendar.getInstance().getTime();

    }

    public ExtendedAuthenticationToken(String authority, String provider, String realm,
            UserAuthenticatedPrincipal principal,
            Authentication token,
            Collection<GrantedAuthority> authorities) {
        super(authorities);
        this.token = token;
        this.principal = principal;
        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
        // set creation time
        this.issueTime = Calendar.getInstance().getTime();

    }

    public Authentication getToken() {
        return token;
    }

    @Override
    public UserAuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return token.isAuthenticated();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {        
        return super.getAuthorities();
    }

    @Override
    public Object getCredentials() {
// no credentials exposed, refer to embedded token
        return null;
    }

    @Override
    public String getName() {
        return token.getName();
    }

    @Override
    public Object getDetails() {
        return token.getDetails();
    }

    @Override
    public void eraseCredentials() {
        if (token instanceof CredentialsContainer) {
            ((CredentialsContainer) token).eraseCredentials();
        }
    }

    public String getAuthority() {
        return authority;
    }

    public String getProvider() {
        return provider;
    }

    public String getRealm() {
        return realm;
    }

    public Date getIssueTime() {
        return issueTime;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new IllegalArgumentException("Cannot set this token to trusted");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((authority == null) ? 0 : authority.hashCode());
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExtendedAuthenticationToken other = (ExtendedAuthenticationToken) obj;
        if (authority == null) {
            if (other.authority != null)
                return false;
        } else if (!authority.equals(other.authority))
            return false;
        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(": ");

        sb.append("Authority: ").append(this.getAuthority()).append("; ");
        sb.append("Provider: ").append(this.getProvider()).append("; ");
// token
        sb.append("Principal: ").append(this.getPrincipal()).append("; ");
        sb.append("Credentials: [PROTECTED]; ");
        sb.append("Authenticated: ").append(this.isAuthenticated()).append("; ");
        sb.append("Details: ").append(this.getDetails()).append("; ");

        if (!this.getAuthorities().isEmpty()) {
            sb.append("Granted Authorities: ");

            int i = 0;
            for (GrantedAuthority authority : this.getAuthorities()) {
                if (i++ > 0) {
                    sb.append(", ");
                }

                sb.append(authority);
            }
        } else {
            sb.append("Not granted any authorities");
        }

        return sb.toString();
    }
}