package it.smartcommunitylab.aac.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import it.smartcommunitylab.aac.core.model.UserIdentity;

public class User {

    // base attributes
    private String subject;
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
    private Collection<UserIdentity> identities;

    // roles are OUTSIDE aac (ie not grantedAuthorities)
    private Set<Role> roles;

    // additional attributes as flatMap
    private Map<String, String> attributes;

    public User(String subject, String username) {
        super();
        this.subject = subject;
        this.username = username;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    public Collection<UserIdentity> getIdentities() {
        return identities;
    }

    public void setIdentities(Collection<UserIdentity> identities) {
        this.identities = identities;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

}
