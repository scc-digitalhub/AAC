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

package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * User details container
 * Wraps identities (account + attributes) along with roles
 * we also collect authorities, but they are effectively handled in authToken
 *
 * This model should be used to describe and manage the real user, in relation to the realm
 * which "owns" the registrations. Its usage is relevant for the auth/securityContext.
 *
 * Services and controllers should adopt the User model.
 */

public class UserDetails implements CredentialsContainer, Serializable {

    // we do not expect this model to be serialized to disk
    // but this could be shared between nodes
    // NOTE: with legacy oauth2 lib auth tokens are serialized to db
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // base attributes
    private final String subjectId;
    private final String realm;
    private String username;

    // identities are stored in a set to de-duplicate
    private final Set<UserIdentity> identities;

    // we extract attributes from identities
    // sets are bound to realm, stored with addressable keys
    // plus we can have additional attributes from external providers
    private final Map<String, UserAttributes> attributes;

    // authorities are roles INSIDE aac (ie user/admin/dev etc)
    // we do not want authorities modified inside session
    // note permission checks are performed on authToken authorities, not here
    private final Set<? extends GrantedAuthority> authorities;

    // we support account status
    private final boolean enabled;
    private final boolean locked;

    public UserDetails(
        String subjectId,
        String realm,
        UserIdentity identity,
        Collection<UserAttributes> attributeSets,
        Collection<? extends GrantedAuthority> authorities
    ) {
        Assert.notNull(subjectId, "subject can not be null");
        Assert.notNull(realm, "realm can not be null");
        Assert.notNull(identity, "one identity is required");

        // subject is our id
        this.subjectId = subjectId;
        this.realm = realm;
        this.username = identity.getAccount().getUsername();

        // identity sets, at minimum we handle first login identity
        this.identities = new HashSet<>();
        addIdentity(identity);

        // attributes sets (outside identities)
        this.attributes = new HashMap<>();
        if (attributeSets != null) {
            for (UserAttributes ras : attributeSets) {
                addAttributeSet(ras);
            }
        }

        // authorities are immutable
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));

        // always enabled at login
        this.enabled = true;
        this.locked = true;
    }

    public UserDetails(
        String subjectId,
        String realm,
        Collection<UserIdentity> identities,
        Collection<UserAttributes> attributeSets,
        Collection<? extends GrantedAuthority> authorities
    ) {
        Assert.notNull(subjectId, "subject can not be null");
        Assert.notNull(realm, "realm can not be null");
        Assert.notEmpty(identities, "one identity is required");

        // subject is our id
        this.subjectId = subjectId;
        this.realm = realm;

        // identity sets, at minimum we handle first login identity
        this.identities = new HashSet<>();
        for (UserIdentity identity : identities) {
            addIdentity(identity);
        }

        // attributes sets (outside identities)
        this.attributes = new HashMap<>();
        for (UserAttributes ras : attributeSets) {
            addAttributeSet(ras);
        }

        // authorities are immutable
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));

        // always enabled at login
        this.enabled = true;
        this.locked = true;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void eraseCredentials() {
        // clear credentials on every identity
        identities
            .stream()
            .forEach(i -> {
                if (i instanceof CredentialsContainer) {
                    ((CredentialsContainer) i).eraseCredentials();
                }
            });
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /*
     * User profile
     */

    public String getSubjectId() {
        return subjectId;
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

    /*
     * Identities we treat them as immutable: we need to ensure list is not
     * modifiable from outside. We expect identities to not be corrupted by
     * consumers
     *
     * Identities should match this user realm
     */

    //    public UserIdentity getIdentity(String userId) {
    //        return identities.get(userId);
    //    }

    public Collection<UserIdentity> getIdentities() {
        return Collections.unmodifiableCollection(identities);
    }

    //    public List<UserIdentity> getIdentities(String realm) {
    //        return Collections.unmodifiableList(identities.values().stream()
    //                .filter(u -> (realm.equals(u.getRealm())))
    //                .collect(Collectors.toList()));
    //    }
    //
    //    public List<UserIdentity> getIdentities(String realm, String provider) {
    //        return Collections.unmodifiableList(identities.values().stream()
    //                .filter(u -> (realm.equals(u.getRealm())
    //                        && provider.equals(u.getProvider())))
    //                .collect(Collectors.toList()));
    //    }

    // add a new identity to current user
    public void addIdentity(UserIdentity identity) {
        addIdentity(identity, true);
    }

    public void addIdentity(UserIdentity identity, boolean replaceIfExists) {
        // realm should match
        if (!realm.equals(identity.getRealm())) {
            throw new IllegalArgumentException("realm does not match");
        }

        if (!replaceIfExists) {
            if (identities.contains(identity)) {
                // skip
                return;
            }
        }
        // we add or replace
        identities.add(identity);

        //        // we also check if profile is incomplete
        //        updateProfile(identity.toBasicProfile());

        // we update username with last set
        this.username = identity.getAccount().getUsername();
    }

    // remove an identity from user
    //    public void eraseIdentity(String userId) {
    //        identities.remove(userId);
    //    }
    //
    //    public void eraseIdentity(UserIdentity identity) {
    //        identities.remove(identity.getUuid());
    //    }
    //
    //    private void updateProfile(BasicProfile p) {
    //        if (profile == null) {
    //            profile = p;
    //            return;
    //        }
    //
    //        // selectively fill missing fields
    //        // first-come first-served
    //        // TODO expose selector for end users to choose identity
    //        if (!StringUtils.hasText(profile.getUsername())) {
    //            profile.setUsername(p.getUsername());
    //        }
    //        if (!StringUtils.hasText(profile.getEmail())) {
    //            profile.setEmail(p.getEmail());
    //        }
    //        if (!StringUtils.hasText(profile.getName())) {
    //            profile.setName(p.getName());
    //        }
    //        if (!StringUtils.hasText(profile.getSurname())) {
    //            profile.setSurname(p.getSurname());
    //        }
    //
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

    /*
     * Attributes
     *
     */
    public UserAttributes getAttributeSet(String setId) {
        return attributes.get(setId);
    }

    public Collection<UserAttributes> getAttributeSets() {
        List<UserAttributes> attrs = new ArrayList<>();
        attrs.addAll(attributes.values());
        identities.forEach(i -> {
            attrs.addAll(i.getAttributes());
        });

        return Collections.unmodifiableCollection(attrs);
    }

    public Collection<UserAttributes> getAttributeSets(boolean excludeIdentities) {
        if (excludeIdentities) {
            return Collections.unmodifiableCollection(attributes.values());
        } else {
            return getAttributeSets();
        }
    }

    public Collection<UserAttributes> getAttributeSets(String realm) {
        return Collections.unmodifiableList(
            attributes.values().stream().filter(u -> (realm.equals(u.getRealm()))).collect(Collectors.toList())
        );
    }

    public Collection<UserAttributes> getAttributeSets(String realm, String provider) {
        return Collections.unmodifiableList(
            getAttributeSets()
                .stream()
                .filter(u -> (realm.equals(u.getRealm()) && provider.equals(u.getProvider())))
                .collect(Collectors.toList())
        );
    }

    // add a new set to current user
    public void addAttributeSet(UserAttributes attributeSet) {
        // realm should match
        if (!realm.equals(attributeSet.getRealm())) {
            throw new IllegalArgumentException("realm does not match");
        }

        // we add or replace
        attributes.put(attributeSet.getAttributesId(), attributeSet);
    }

    // remove a set from user
    public void removeAttributeSet(UserAttributes attributeSet) {
        attributes.remove(attributeSet.getAttributesId());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    @Override
    public String toString() {
        return "User [subjectId=" + subjectId + ", authorities=" + authorities + "]";
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
