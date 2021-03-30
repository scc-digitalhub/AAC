package it.smartcommunitylab.aac.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;

public class User {

    // base attributes
    private String subject;
    private String realm;

    // basic profile
    private String name;
    private String surname;
    private String username;
    private String email;

    // identities associated with this user
    // this will be populated as needed, make no assumption about being always set
    // or complete: for example the only identity provided could be the one
    // selected for the authentication request, or those managed by a given
    // authority etc
    private Set<UserIdentity> identities;

    // roles are OUTSIDE aac (ie not grantedAuthorities)
    // roles are associated to USER(=subjectId) not single identities/realms
    // this field should be used for caching, consumers should refresh
    // otherwise we should implement an (external) expiring + refreshing cache with
    // locking
    private Set<SpaceRole> roles;

    // additional attributes as UserAttributes collection
    private Set<UserAttributes> attributes;

    public User(String subject, String realm) {
        super();
        this.subject = subject;
        this.realm = realm;
        this.identities = new HashSet<>();
        this.attributes = new HashSet<>();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<UserIdentity> getIdentities() {
        return identities;
    }

    public void setIdentities(Set<UserIdentity> identities) {
        this.identities = identities;
    }

    public void addIdentity(UserIdentity identity) {
        identities.add(identity);
    }

    public Set<UserAttributes> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<UserAttributes> attributes) {
        this.attributes = attributes;
    }

    public void addAttributes(UserAttributes attributes) {
        this.attributes.add(attributes);
    }

    /*
     * Roles are mutable and comparable
     */

    public Set<SpaceRole> getRoles() {
        return roles;
    }

    public void setRoles(Collection<SpaceRole> rr) {
        this.roles = new HashSet<>();
        addRoles(rr);
    }

    public void addRoles(Collection<SpaceRole> rr) {
        roles.addAll(rr);
    }

    public void removeRoles(Collection<SpaceRole> rr) {
        roles.removeAll(rr);
    }

    public void addRole(SpaceRole r) {
        this.roles.add(r);
    }

    public void removeRole(SpaceRole r) {
        this.roles.remove(r);
    }

}
