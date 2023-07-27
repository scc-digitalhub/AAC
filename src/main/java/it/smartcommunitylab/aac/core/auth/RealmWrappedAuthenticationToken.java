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
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class RealmWrappedAuthenticationToken extends WrappedAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String realm;

    public RealmWrappedAuthenticationToken(AbstractAuthenticationToken token, String realm) {
        this(token, realm, null);
    }

    public RealmWrappedAuthenticationToken(AbstractAuthenticationToken token, String realm, String authority) {
        super(token);
        Assert.hasText(realm, "realm can not be null or empty");
        this.token = token;
        this.authority = authority;
        this.realm = realm;
    }

    public String getAuthority() {
        return authority;
    }

    public String getRealm() {
        return realm;
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
        int result = 1;
        result = prime * result + ((authority == null) ? 0 : authority.hashCode());
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RealmWrappedAuthenticationToken other = (RealmWrappedAuthenticationToken) obj;
        if (authority == null) {
            if (other.authority != null) return false;
        } else if (!authority.equals(other.authority)) return false;
        if (realm == null) {
            if (other.realm != null) return false;
        } else if (!realm.equals(other.realm)) return false;
        if (token == null) {
            if (other.token != null) return false;
        } else if (!token.equals(other.token)) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(": ");

        sb.append("Realm: ").append(this.getRealm()).append("; ");
        sb.append("Authority: ").append(this.getAuthority()).append("; ");
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
