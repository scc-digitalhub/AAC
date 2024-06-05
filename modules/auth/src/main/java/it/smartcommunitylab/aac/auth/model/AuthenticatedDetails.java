/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.auth.model;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

@Getter
@ToString
public abstract class AuthenticatedDetails implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // base attributes
    protected final String id;
    protected final String realm;
    protected final String name;

    // authorities are roles INSIDE aac (ie user/admin/dev etc)
    // we do not want authorities modified inside session
    // note permission checks are performed on authToken authorities, not here
    protected final Set<? extends GrantedAuthority> authorities;

    protected AuthenticatedDetails(
        String id,
        String realm,
        String name,
        Collection<? extends GrantedAuthority> authorities
    ) {
        Assert.notNull(id, "userId can not be null");
        Assert.notNull(realm, "realm can not be null");
        Assert.notNull(name, "username can not be null");

        // base info
        this.id = id;
        this.realm = realm;
        this.name = name;

        // authorities are immutable
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));
    }

    public boolean hasAuthority(String auth) {
        return getAuthorities() != null && getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(auth));
    }

    public boolean hasAnyAuthority(String... auth) {
        List<String> set = Arrays.asList(auth);
        return getAuthorities() != null && getAuthorities().stream().anyMatch(a -> set.contains(a.getAuthority()));
    }

    public boolean isRealmDeveloper() {
        // TODO check if can do better at the level of user
        return (
            getAuthorities() != null &&
            getAuthorities()
                .stream()
                .anyMatch(
                    a ->
                        Config.R_ADMIN.equals(a.getAuthority()) ||
                        isRealmRole(a.getAuthority(), Config.R_ADMIN) ||
                        isRealmRole(a.getAuthority(), Config.R_DEVELOPER)
                )
        );
    }

    public boolean isRealmAdmin() {
        // TODO check if can do better at the level of user
        return (
            getAuthorities() != null &&
            getAuthorities()
                .stream()
                .anyMatch(a -> Config.R_ADMIN.equals(a.getAuthority()) || isRealmRole(a.getAuthority(), Config.R_ADMIN))
        );
    }

    public boolean isSystemDeveloper() {
        // any system user is a dev
        return SystemKeys.REALM_SYSTEM.equals(getRealm());
    }

    public boolean isSystemAdmin() {
        // TODO check if can do better at the level of user
        return (
            getAuthorities() != null && getAuthorities().stream().anyMatch(a -> Config.R_ADMIN.equals(a.getAuthority()))
        );
    }

    public Collection<String> getRealms() {
        return getAuthorities()
            .stream()
            .filter(a -> a.getAuthority().contains(":"))
            .map(a -> a.getAuthority().split(":")[0])
            .collect(Collectors.toSet());
    }

    protected boolean isRealmRole(String authority, String role) {
        return authority.endsWith(':' + role);
    }

    /*
     * Helpers
     */

    protected static SortedSet<GrantedAuthority> sortAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "Cannot pass a null GrantedAuthority collection");
        // Ensure array iteration order is predictable (as per
        // UserDetails.getAuthorities() contract and SEC-717)
        SortedSet<GrantedAuthority> sortedAuthorities = new TreeSet<>(new AuthorityComparator());

        for (GrantedAuthority grantedAuthority : authorities) {
            Assert.notNull(grantedAuthority, "GrantedAuthority list cannot contain any null elements");
            sortedAuthorities.add(grantedAuthority);
        }

        return sortedAuthorities;
    }

    static class AuthorityComparator implements Comparator<GrantedAuthority>, Serializable {

        private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

        public int compare(GrantedAuthority g1, GrantedAuthority g2) {
            // Neither should ever be null as each entry is checked before adding it to
            // the set.
            // If the authority is null, it is a custom authority and should precede
            // others.
            if (g2.getAuthority() == null) {
                return -1;
            }

            if (g1.getAuthority() == null) {
                return 1;
            }

            return g1.getAuthority().compareTo(g2.getAuthority());
        }
    }
}
