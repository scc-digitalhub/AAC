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

package it.smartcommunitylab.aac.users.model;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * User details container
 *
 * This model should be used to describe the user in the auth/securityContext.
 *
 * Services and controllers should adopt the User model.
 */

public class UserDetails implements Serializable {

    // we do not expect this model to be serialized to disk
    // but this could be shared between nodes
    // NOTE: with legacy oauth2 lib auth tokens are serialized to db
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // base attributes
    private final String userId;
    private final String realm;
    private final String username;

    // authorities are roles INSIDE aac (ie user/admin/dev etc)
    // we do not want authorities modified inside session
    // note permission checks are performed on authToken authorities, not here
    private final Set<? extends GrantedAuthority> authorities;

    // we support account status
    private final boolean enabled;
    private final boolean locked;

    public UserDetails(
        String userId,
        String realm,
        String username,
        Collection<? extends GrantedAuthority> authorities
    ) {
        Assert.notNull(userId, "userId can not be null");
        Assert.notNull(realm, "realm can not be null");

        // subject is our id
        this.userId = userId;
        this.realm = realm;
        this.username = username;

        // authorities are immutable
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));

        // always enabled at login
        this.enabled = true;
        this.locked = true;
    }

    public String getUsername() {
        return username;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /*
     * User profile
     */

    public String getUserId() {
        return userId;
    }

    public String getRealm() {
        return realm;
    }

    //
    //    public String getFirstName() {
    //        return profile != null ? profile.getName() : "";
    //    }
    //
    //    public String getLastName() {
    //        return profile != null ? profile.getSurname() : "";
    //    }
    //
    //    public String getFullName() {
    //        StringBuilder sb = new StringBuilder();
    //        String firstName = getFirstName();
    //        String lastName = getLastName();
    //        if (StringUtils.hasText(firstName)) {
    //            sb.append(firstName).append(" ");
    //        }
    //        if (StringUtils.hasText(lastName)) {
    //            sb.append(lastName);
    //        }
    //
    //        return sb.toString().trim();
    //
    //    }
    //
    //    public String getEmailAddress() {
    //        return profile != null ? profile.getEmail() : "";
    //    }

    //    public BasicProfile getBasicProfile() {
    //        // return a copy to avoid mangling
    //        return new BasicProfile(profile);
    //    }

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
                .anyMatch(a ->
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
            .filter(a -> {
                return (a instanceof RealmGrantedAuthority);
            })
            .map(a -> {
                return ((RealmGrantedAuthority) a).getRealm();
            })
            .collect(Collectors.toSet());
    }

    private boolean isRealmRole(String authority, String role) {
        return authority.endsWith(':' + role);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    @Override
    public String toString() {
        return "UserDetails [userId=" + userId + ", authorities=" + authorities + "]";
    }

    /*
     * Helpers
     */

    private static SortedSet<GrantedAuthority> sortAuthorities(Collection<? extends GrantedAuthority> authorities) {
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

    private static class AuthorityComparator implements Comparator<GrantedAuthority>, Serializable {

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
