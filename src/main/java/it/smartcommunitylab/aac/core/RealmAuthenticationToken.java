package it.smartcommunitylab.aac.core;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;

public class RealmAuthenticationToken implements Authentication,
        CredentialsContainer {

    private static final long serialVersionUID = -1302725087208017064L;

    private final String realm;
    private final String authority;
    private final String provider;

    private AbstractAuthenticationToken token;

    public RealmAuthenticationToken(String realm, String authority, String provider,
            AbstractAuthenticationToken token) {
        this.token = token;
        this.realm = realm;
        this.authority = authority;
        this.provider = provider;

    }

    @Override
    public boolean isAuthenticated() {
        return token.isAuthenticated();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return token.getAuthorities();
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
        token.eraseCredentials();
    }

    @Override
    public Object getPrincipal() {
        return token.getPrincipal();
    }

    public String getRealm() {
        return realm;
    }

    public String getAuthority() {
        return authority;
    }

    public String getProvider() {
        return provider;
    }

    public AbstractAuthenticationToken getAuthenticationToken() {
        return token;
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
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
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
        RealmAuthenticationToken other = (RealmAuthenticationToken) obj;
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
        if (realm == null) {
            if (other.realm != null)
                return false;
        } else if (!realm.equals(other.realm))
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

        sb.append("Realm: ").append(this.getRealm()).append("; ");
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
