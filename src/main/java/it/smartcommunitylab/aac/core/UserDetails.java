package it.smartcommunitylab.aac.core;

import java.io.Serializable;
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
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

/*
 * User details container
 * Wraps identities (account + attributes) along with roles 
 * we also collect authorities, but they are effectively handled in authToken
 */

//TODO evaluate remove spring Userdetails compatibility, we don't need it 
public class UserDetails implements org.springframework.security.core.userdetails.UserDetails, CredentialsContainer {

    // we do not expect this model to be serialized to disk
    // but this could be shared between nodes
    private static final long serialVersionUID = 3605707677727615074L;

    // base attributes
    private final String subjectId;

    // this is mutable, we want to keep one "identity" mapped
    // TODO evaluate removal from here, should be managed only ouside authentication
    private BasicProfile profile;

    // identities are stored with addressable keys (userId)
    private final Map<String, UserIdentity> identities;

    // we extract attributes from identities
    // sets are bound to realm, stored with addressable keys
    // plus we can have additional attributes from external providers
    // TODO
    private final Map<String, UserAttributes> attributes;

    // authorities are roles INSIDE aac (ie user/admin/dev etc)
    // we do not want authorities modified inside session
    // note permission checks are performed on authToken authorities, not here
    // TODO remove, should be left in token, we keep for interface compatibiilty
    private final Set<GrantedAuthority> authorities;

    // we don't support account enabled/disabled
    private final boolean enabled;

    public UserDetails(
            String subjectId,
            UserIdentity identity,
            Collection<UserAttributes> attributeSets,
            Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(subjectId, "subject can not be null");
        Assert.notNull(identity, "one identity is required");

        // subject is our id
        this.subjectId = subjectId;

        // identity sets, at minimum we handle first login identity
        this.identities = new HashMap<>();
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

        // always enabled
        this.enabled = true;
    }

    public UserDetails(
            String subjectId,
            Collection<UserIdentity> identities,
            Collection<UserAttributes> attributeSets,
            Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(subjectId, "subject can not be null");
        Assert.notEmpty(identities, "one identity is required");

        // subject is our id
        this.subjectId = subjectId;

        // identity sets, at minimum we handle first login identity
        this.identities = new HashMap<>();
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
        identities.values().stream()
                .forEach(i -> i.eraseCredentials());

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

    public String getSubjectId() {
        return subjectId;
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

    public BasicProfile getBasicProfile() {
        // return a copy to avoid mangling
        return new BasicProfile(profile);
    }

    /*
     * Identities we treat them as immutable: we need to ensure list is not
     * modifiable from outside. We expect identities to not be corrupted by
     * consumers
     */

    public UserIdentity getIdentity(String userId) {
        return identities.get(userId);
    }

    public Collection<UserIdentity> getIdentities() {
        return Collections.unmodifiableCollection(identities.values());
    }

    public List<UserIdentity> getIdentities(String realm) {
        return Collections.unmodifiableList(identities.values().stream()
                .filter(u -> realm.equals(u.getRealm()))
                .collect(Collectors.toList()));
    }

    public List<UserIdentity> getIdentities(String realm, String authority) {
        return Collections.unmodifiableList(identities.values().stream()
                .filter(u -> (realm.equals(u.getRealm()) && authority.equals(u.getAuthority())))
                .collect(Collectors.toList()));
    }

    public List<UserIdentity> getIdentities(String realm, String authority, String provider) {
        return Collections.unmodifiableList(identities.values().stream()
                .filter(u -> (realm.equals(u.getRealm()) && authority.equals(u.getAuthority())
                        && provider.equals(u.getProvider())))
                .collect(Collectors.toList()));
    }

    public boolean hasAnyIdentity(String realm) {
        return identities.values().stream().anyMatch(u -> realm.equals(u.getRealm()));
    }

    // add a new identity to current user
    public void addIdentity(UserIdentity identity) {

        // we add or replace
        identities.put(identity.getUserId(), identity);

        // we also check if profile is incomplete
        updateProfile(identity.toProfile());
    }

    // remove an identity from user
    public void eraseIdentity(String userId) {
        identities.remove(userId);
    }

    public void eraseIdentity(UserIdentity identity) {
        identities.remove(identity.getUserId());
    }

    private void updateProfile(BasicProfile p) {
        if (profile == null) {
            profile = p;
            return;
        }

        // selectively fill missing fields
        // first-come first-served
        // TODO expose selector for end users to choose identity
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
     * Attributes
     */
    public Collection<UserAttributes> getAttributeSets() {
        return Collections.unmodifiableCollection(attributes.values());
    }

    public Collection<UserAttributes> getAttributeSets(String realm) {
        String baseKey = realm + "|";
        Set<String> keys = attributes.keySet().stream().filter(k -> k.startsWith(baseKey)).collect(Collectors.toSet());
        return Collections.unmodifiableSet(attributes.entrySet().stream()
                .filter(e -> keys.contains(e.getKey()))
                .map(e -> e.getValue())
                .collect(Collectors.toSet()));

    }

    // we enforce a single set per realm+authority+provider+identifier
    public UserAttributes getAttributeSet(String realm, String authority, String provider, String identifier) {
        String key = realm + "|" + authority + "|" + provider + "|" + identifier;
        return attributes.get(key);
    }

    // add a new set to current user
    public void addAttributeSet(UserAttributes attributeSet) {
        String key = attributeSet.getRealm() + "|" + attributeSet.getAuthority() + "|" + attributeSet.getProvider()
                + "|" + attributeSet.getIdentifier();

        // we add or replace
        attributes.put(key, attributeSet);

    }

    // remove a set from user
    public void eraseAttributeSet(UserAttributes attributeSet) {
        String key = attributeSet.getRealm() + "|" + attributeSet.getAuthority() + "|" + attributeSet.getProvider()
                + "|" + attributeSet.getIdentifier();
        attributes.remove(key);
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
        return "User [subjectId=" + subjectId + ", authorities=" + authorities + "]";
    }

    /*
     * Helpers
     */

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
