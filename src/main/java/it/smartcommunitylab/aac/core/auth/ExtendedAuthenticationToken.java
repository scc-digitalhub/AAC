/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;

/*
 * An authenticationToken holding both the provider token and a resolved identity
 *
 */

public class ExtendedAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final UserAuthenticatedPrincipal principal;

    private final String authority;
    private final String provider;
    private final String realm;

    private Authentication token;

    private final Instant issuedAt;
    private final Instant expiresAt;

    // TODO add validity checks

    public ExtendedAuthenticationToken(
        String authority,
        String provider,
        String realm,
        UserAuthenticatedPrincipal principal,
        Authentication token
    ) {
        super(Collections.emptyList());
        this.token = token;
        this.principal = principal;
        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
        // set creation time
        this.issuedAt = Instant.now();
        this.expiresAt = null;
    }

    public ExtendedAuthenticationToken(
        String authority,
        String provider,
        String realm,
        UserAuthenticatedPrincipal principal,
        Authentication token,
        Instant expiresAt
    ) {
        super(Collections.emptyList());
        this.token = token;
        this.principal = principal;
        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
        // set creation time
        this.issuedAt = Instant.now();
        this.expiresAt = expiresAt;

        if (expiresAt != null && expiresAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("expired authentication");
        }
    }

    public ExtendedAuthenticationToken(
        String authority,
        String provider,
        String realm,
        UserAuthenticatedPrincipal principal,
        Authentication token,
        Collection<GrantedAuthority> authorities
    ) {
        super(authorities);
        this.token = token;
        this.principal = principal;
        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
        // set creation time
        this.issuedAt = Instant.now();
        this.expiresAt = null;
    }

    public ExtendedAuthenticationToken(
        String authority,
        String provider,
        String realm,
        UserAuthenticatedPrincipal principal,
        Authentication token,
        Instant expiresAt,
        Collection<GrantedAuthority> authorities
    ) {
        super(authorities);
        this.token = token;
        this.principal = principal;
        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
        // set creation time
        this.issuedAt = Instant.now();
        this.expiresAt = expiresAt;

        if (expiresAt != null && expiresAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("expired authentication");
        }
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
        return principal.getName();
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

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getAge() {
        if (issuedAt != null) {
            return Duration.between(issuedAt, Instant.now()).getSeconds();
        }
        return -1;
    }

    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }

        return expiresAt.isBefore(Instant.now());
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
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        ExtendedAuthenticationToken other = (ExtendedAuthenticationToken) obj;
        if (authority == null) {
            if (other.authority != null) return false;
        } else if (!authority.equals(other.authority)) return false;
        if (provider == null) {
            if (other.provider != null) return false;
        } else if (!provider.equals(other.provider)) return false;
        if (token == null) {
            if (other.token != null) return false;
        } else if (!token.equals(other.token)) return false;
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
