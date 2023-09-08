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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import jakarta.validation.Valid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RealmGrantedAuthority implements GrantedAuthority {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String realm;
    private final String role;

    public RealmGrantedAuthority(String realm, String role) {
        Assert.hasText(realm, "A space textual representation is required");
        Assert.hasText(role, "A granted authority textual representation is required");
        this.realm = realm;
        this.role = role;
    }

    protected RealmGrantedAuthority() {
        this.realm = null;
        this.role = null;
    }

    public String getRealm() {
        return realm;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String getAuthority() {
        return realm + ":" + role;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RealmGrantedAuthority other = (RealmGrantedAuthority) obj;
        if (role == null) {
            if (other.role != null) return false;
        } else if (!role.equals(other.role)) return false;
        if (realm == null) {
            if (other.realm != null) return false;
        } else if (!realm.equals(other.realm)) return false;
        return true;
    }

    @Override
    public String toString() {
        return getAuthority();
    }
}
