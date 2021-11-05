package it.smartcommunitylab.aac.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;

/*
 * A model describing the user outside the auth/security context.
 * 
 * Can be safely used to manage attributes/properties, and also roles both in 
 * same-realm and cross-realm scenarios.
 * 
 * Do note that in cross realm managers and builders should properly handle private attributes and 
 * disclose only appropriate identities/properties to consumers.
 */
@JsonInclude(Include.NON_NULL)
public class User {

    @NotBlank
    private final String subjectId;

    // describes the realm responsible for this user
    // TODO remove field, not needed with realm-scoped resources
    private final String source;

    // realm describes the current user for the given realm
    // TODO always set to subject source realm
    private String realm;

    // basic profile
    private String username;
    private String email;

    // user status
    // TODO convert to enum
    private boolean blocked;
    private boolean locked;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date expirationDate;

    // audit
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date createDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date modifiedDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date loginDate;
    private String loginIp;
    private String loginProvider;

    // authorities in AAC
    // these are either global or realm scoped
    private Set<GrantedAuthority> authorities;

    // identities associated with this user (realm scoped)
    // this will be populated as needed, make no assumption about being always set
    // or complete: for example the only identity provided could be the one
    // selected for the authentication request, or those managed by a given
    // authority etc
    // these could also be empty in cross-realm scenarios
    private Set<UserIdentity> identities;

    // roles are OUTSIDE aac (ie not grantedAuthorities)
    // roles are associated to USER(=subjectId) not single identities/realms
    // realm scoped
    @JsonProperty("roles")
    private Set<RealmRole> realmRoles;

    // space roles are global
    private Set<SpaceRole> spaceRoles;

    // additional attributes as UserAttributes collection
    // realm scoped
    private List<UserAttributes> attributes;

    public User(String subjectId, String source) {
        Assert.hasText(subjectId, "subject can not be null or empty");
        Assert.notNull(source, "source realm can not be null");

        this.subjectId = subjectId;
        this.source = source;
        // set consuming realm to source
        this.realm = source;
        this.authorities = Collections.emptySet();
        this.identities = new HashSet<>();
        this.attributes = new ArrayList<>();
        this.realmRoles = new HashSet<>();
        this.spaceRoles = new HashSet<>();
    }

    public User(UserDetails details) {
        Assert.notNull(details, "user details can not be null");
        this.subjectId = details.getSubjectId();
        this.source = details.getRealm();
        // set consuming realm to source
        this.realm = source;
        this.authorities = details.getAuthorities().stream()
                .filter(a -> (a instanceof RealmGrantedAuthority))
                .map(a -> (RealmGrantedAuthority) a)
                .collect(Collectors.toSet());
        this.identities = new HashSet<>(details.getIdentities());
        this.attributes = new ArrayList<>(details.getAttributeSets(false));
        this.spaceRoles = new HashSet<>();

        this.username = details.getUsername();
//        this.name = details.getFirstName();
//        this.surname = details.getLastName();
//        this.email = details.getEmailAddress();

    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getSource() {
        return source;
    }

    public Set<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<GrantedAuthority> authorities) {
        this.authorities = new HashSet<>();
        if (authorities != null) {
            this.authorities.addAll(authorities);
        }
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getSurname() {
//        return surname;
//    }
//
//    public void setSurname(String surname) {
//        this.surname = surname;
//    }

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

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public String getLoginProvider() {
        return loginProvider;
    }

    public void setLoginProvider(String loginProvider) {
        this.loginProvider = loginProvider;
    }

    public Collection<UserIdentity> getIdentities() {
        return Collections.unmodifiableCollection(identities);
    }

    public void setIdentities(Collection<UserIdentity> identities) {
        this.identities = new HashSet<>();
        if (identities != null) {
            this.identities.addAll(identities);
            // add all attributes
            identities.forEach(i -> attributes.addAll(i.getAttributes()));
        }

    }

    public void addIdentity(UserIdentity identity) {
        identities.add(identity);

        // add all attributes
        identities.forEach(i -> attributes.addAll(i.getAttributes()));
    }

    public Collection<UserAttributes> getAttributes() {
        return Collections.unmodifiableCollection(attributes);
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = new ArrayList<>();
        if (attributes != null) {
            this.attributes.addAll(attributes);
        }
    }

    public void addAttributes(Collection<UserAttributes> attributes) {
        this.attributes.addAll(attributes);
    }

    public void addAttributes(UserAttributes attributes) {
        this.attributes.add(attributes);
    }

    /*
     * Authorities
     * 
     */

    /*
     * Roles are mutable and comparable
     */
    public Set<RealmRole> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(Collection<RealmRole> rr) {
        this.realmRoles = new HashSet<>();
        if (rr != null) {
            realmRoles.addAll(rr);
        }
    }

    /*
     * Space roles
     */
    public Set<SpaceRole> getSpaceRoles() {
        return spaceRoles;
    }

    public void setSpaceRoles(Collection<SpaceRole> rr) {
        this.spaceRoles = new HashSet<>();
        if (rr != null) {
            spaceRoles.addAll(rr);
        }
    }

//    public void addSpaceRoles(Collection<SpaceRole> rr) {
//        if (rr != null) {
//            spaceRoles.addAll(rr);
//        }
//    }
//
//    public void removeSpaceRoles(Collection<SpaceRole> rr) {
//        spaceRoles.removeAll(rr);
//    }
//
//    public void addSpaceRole(SpaceRole r) {
//        this.spaceRoles.add(r);
//    }
//
//    public void removeSpaceRole(SpaceRole r) {
//        this.spaceRoles.remove(r);
//    }

}
