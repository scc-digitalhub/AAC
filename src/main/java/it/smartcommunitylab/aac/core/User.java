package it.smartcommunitylab.aac.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

/*
 * User details container
 * Wraps identities (account + attributes) along with roles 
 * we also collect authorities, but they are effectively handled in authToken
 */

public class User implements UserDetails, CredentialsContainer {

    // we do not expect this model to be serialized to disk
    // but this could be shared between nodes
    private static final long serialVersionUID = 3605707677727615074L;

    // base attributes
    private final String subject;

    // this is mutable, we want to keep one identity mapped
    private BasicProfile profile;

    // identities are grouped by realm
    private Map<String, Collection<UserIdentity>> identities;
    private static final String GLOBAL_REALM = "_NULL_REALM";

    // authorities are roles INSIDE aac (ie user/admin/dev etc)
    // we do not want authorities modified inside session
    // note permission checks are performed on authToken authorities, not here
    private final Set<GrantedAuthority> authorities;

    // roles are OUTSIDE aac
    // these can be modified
    // roles are associated to USER not single identities
    // this field will be used for caching only
    private Set<Role> roles;

    // we don't support account enabled/disabled now
    private final boolean enabled;

    public User(String subject, UserIdentity identity, Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(subject, "subject can not be null");
        Assert.notNull(identity, "active identity is required");

        // subject is our id
        this.subject = subject;

        // identity sets, at minimum we handle first login identity
        this.identities = new HashMap<>();
        addIdentity(identity);

        // also map to profile
        profile = identity.toProfile();

        // authorities are immutable
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));

        // roles should be updated after login, or fetched during execution
        this.roles = new HashSet<>();

        // always enabled
        this.enabled = true;
    }

    @Override
    public String getUsername() {
        return profile.getUsername();
    }

    @Override
    public void eraseCredentials() {
        // clear credentials on every identity
        for (Collection<UserIdentity> ids : identities.values()) {
            ids.stream()
                    .forEach(i -> i.eraseCredentials());
        }
    }

    @Override
    public String getPassword() {
        // no password here
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /*
     * User profile
     */

    public String getSubject() {
        return subject;
    }

    public String getFirstName() {
        return profile != null ? profile.getName() : "";
    }

    public String getLastName() {
        return profile != null ? profile.getSurname() : "";
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        String firstName = getFirstName();
        String lastName = getLastName();
        if (StringUtils.hasText(firstName)) {
            sb.append(firstName).append(" ");
        }
        if (StringUtils.hasText(lastName)) {
            sb.append(lastName);
        }

        return sb.toString().trim();

    }

    public String getEmailAddress() {
        return profile != null ? profile.getEmail() : "";
    }

    /*
     * Identities: we treat them as immutable by serving copies to getters
     */

    public Collection<UserIdentity> getIdentities(String realm) {
        if (identities.containsKey(realm)) {
            return immutableCopy(identities.get(realm));
        } else {
            return null;
        }
    }

    public UserIdentity getIdentity(String realm, String authority, String provider) {
        UserIdentity id = null;
        if (identities.containsKey(realm)) {
            Optional<UserIdentity> o = identities.get(realm).stream()
                    .filter(i -> authority.equals(i.getAuthority()) && provider.equals(i.getProvider()))
                    .findFirst();
            if (o.isPresent()) {
                id = o.get();
            }
        }

        return id;
    }

    public boolean hasAnyIdentity(String realm) {
        return identities.containsKey(realm);
    }

    // add a new identity to current user
    public void addIdentity(UserIdentity identity) {
        // check if global realm
        String realm = identity.getRealm();
        if (realm == null) {
            realm = GLOBAL_REALM;
        }

        if (!identities.containsKey(realm)) {
            identities.put(realm, new HashSet<>());
        }

        // we add or replace
        identities.get(realm).add(identity);

        // we also check if profile is incomplete
        updateProfile(identity.toProfile());
    }

    // remove an identity from user
    public void clearIdentity(UserIdentity identity) {
        // check if global realm
        String realm = identity.getRealm();
        if (realm == null) {
            realm = GLOBAL_REALM;
        }

        if (identities.containsKey(realm)) {
            identities.get(realm).remove(identity);
        }
    }

    private void updateProfile(BasicProfile p) {
        if (profile == null) {
            profile = p;
            return;
        }

        // selectively fill missing fields
        if (!StringUtils.hasText(profile.getUsername())) {
            profile.setUsername(p.getUsername());
        }
        if (!StringUtils.hasText(profile.getEmail())) {
            profile.setEmail(p.getEmail());
        }
        if (!StringUtils.hasText(profile.getName())) {
            profile.setName(p.getName());
        }
        if (!StringUtils.hasText(profile.getSurname())) {
            profile.setSurname(p.getSurname());
        }

    }

    /*
     * Roles are mutable and comparable
     */

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Collection<Role> rr) {
        this.roles = new HashSet<>();
        addRoles(rr);
    }

    public void addRoles(Collection<Role> rr) {
        roles.addAll(rr);
    }

    public void removeRoles(Collection<Role> rr) {
        roles.removeAll(rr);
    }

    public void addRole(Role r) {
        this.roles.add(r);
    }

    public void removeRole(Role r) {
        this.roles.remove(r);
    }

    /*
     * not supported
     */

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;

    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;

    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "User [subject=" + subject + ", authorities=" + authorities + "]";
    }

    /*
     * Helpers
     */

    private static Set<UserIdentity> immutableCopy(Collection<UserIdentity> s) {
        // we assume identities are immutable
        // otherwise we could deep copy via serde
        Set<UserIdentity> set = new HashSet<>();
        set.addAll(s);
        return Collections.unmodifiableSet(set);
    }

    private static SortedSet<GrantedAuthority> sortAuthorities(
            Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "Cannot pass a null GrantedAuthority collection");
        // Ensure array iteration order is predictable (as per
        // UserDetails.getAuthorities() contract and SEC-717)
        SortedSet<GrantedAuthority> sortedAuthorities = new TreeSet<>(
                new AuthorityComparator());

        for (GrantedAuthority grantedAuthority : authorities) {
            Assert.notNull(grantedAuthority,
                    "GrantedAuthority list cannot contain any null elements");
            sortedAuthorities.add(grantedAuthority);
        }

        return sortedAuthorities;
    }

    private static class AuthorityComparator implements Comparator<GrantedAuthority>,
            Serializable {
        private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

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
