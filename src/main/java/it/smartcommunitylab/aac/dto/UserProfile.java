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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.roles.model.SpaceRole;
import it.smartcommunitylab.aac.users.model.User;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;

/**
 * Describe a user in a reduced form, used for processing and profile
 * generation.
 *
 * TODO refactor
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

    // realm
    private String realm;

    // basic profile
    private String username;

    private Set<String> groups;
    private Set<String> roles;
    // private Set<String> spaceRoles;

    // attributes are exposed as sets to keep identifiers private
    private Set<AttributeSet> attributes;

    public UserProfile() {
        this.subjectId = null;
        this.realm = null;
    }

    public UserProfile(User user) {
        this.subjectId = user.getUserId();
        this.realm = user.getRealm();
        this.username = user.getUsername();

        Set<String> groups = user.getGroups() != null
            ? user.getGroups().stream().map(g -> g.getGroup()).collect(Collectors.toSet())
            : null;
        setGroups(groups);

        Set<String> roles = user.getRoles() != null
            ? user.getRoles().stream().map(r -> r.getRole()).collect(Collectors.toSet())
            : null;
        setRoles(roles);
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

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
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

    // public Set<String> getSpaceRoles() {
    //     return spaceRoles;
    // }

    // public void setSpaceRoles(Set<String> spaceRoles) {
    //     this.spaceRoles = roles;
    // }

    // public void setSpaceRoles(Collection<SpaceRole> spaceRoles) {
    //     this.spaceRoles = spaceRoles.stream().map(r -> r.getAuthority()).collect(Collectors.toSet());
    // }

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
}
