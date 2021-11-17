/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.dto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;

import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.User;

/**
 * Describe a user in a reduced form, used for processing and profile
 * generation.
 * 
 * @author raman
 *
 */

@JsonInclude(Include.NON_EMPTY)
public class UserProfile {

    // base attributes
    // always set
    @NotBlank
    private final String subjectId;

    // describes the realm responsible for this user
    private final String source;

    // realm describes the current user for the given realm
    private String realm;

    // basic profile
    private String username;

    // roles are translated as authorities
    private Set<String> authorities;

    private Set<String> roles;
    private Set<String> spaceRoles;

    // attributes are exposed as sets to keep identifiers private
    private Set<AttributeSet> attributes;

    public UserProfile() {
        this.subjectId = null;
        this.source = null;
    }

    public UserProfile(User user) {
        this.subjectId = user.getSubjectId();
        this.source = user.getSource();
        this.realm = user.getRealm();
        this.username = user.getUsername();

        setAuthorities(user.getAuthorities());
        setRoles(user.getRealmRoles());
        setAttributes(user.getAttributes());
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }

    public void setAuthorities(Collection<GrantedAuthority> authorities) {
        this.authorities = authorities.stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void setRoles(Collection<RealmRole> roles) {
        this.roles = roles.stream().map(r -> r.getAuthority()).collect(Collectors.toSet());
    }

    public Set<String> getSpaceRoles() {
        return spaceRoles;
    }

    public void setSpaceRoles(Set<String> spaceRoles) {
        this.spaceRoles = roles;
    }

    public void setSpaceRoles(Collection<SpaceRole> spaceRoles) {
        this.spaceRoles = spaceRoles.stream().map(r -> r.getAuthority()).collect(Collectors.toSet());
    }

    public Set<AttributeSet> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<AttributeSet> attributes) {
        this.attributes = attributes;
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = new HashSet<>(attributes);
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getSource() {
        return source;
    }

}
