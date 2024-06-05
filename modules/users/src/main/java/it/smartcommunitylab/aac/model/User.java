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

package it.smartcommunitylab.aac.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.users.model.UserDetails;
import it.smartcommunitylab.aac.users.model.UserStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * A model describing the user outside the auth/security context.
 *
 * In addition to core properties, user resources can be included.
 */
@ToString
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(value = { "id", "type" }, ignoreUnknown = true)
public class User implements UserResourceContext {

    @NotBlank
    private final String userId;

    @NotBlank
    private final String realm;

    // basic profile
    private String username;
    private String email;
    private boolean emailVerified;

    private String lang = Locale.ENGLISH.getLanguage();

    // user status
    private UserStatus status;

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

    //TODO remove
    private Boolean tosAccepted;

    // authorities in AAC
    // these are either global or realm scoped
    // stored here because these are NOT resources
    private Set<GrantedAuthority> authorities;

    // // identities associated with this user (realm scoped)
    // // this will be populated as needed, make no assumption about being always set
    // // or complete: for example the only identity provided could be the one
    // // selected for the authentication request, or those managed by a given
    // // authority etc
    // // these could also be empty in cross-realm scenarios
    // private Set<UserIdentity> identities;

    // // roles are OUTSIDE aac (ie not grantedAuthorities)
    // // roles are associated to USER(=subjectId) not single identities/realms
    // // realm scoped
    // @JsonProperty("roles")
    // private Set<RealmRole> realmRoles;

    // // space roles are global
    // private Set<SpaceRole> spaceRoles;

    // // groups where user is a member
    // private Set<Group> groups;

    // // additional attributes as UserAttributes collection
    // // realm scoped
    // private List<UserAttributes> attributes;

    // // terms of service.
    // private Boolean tosAccepted;

    // resources stored as map context and read via accessors

    private Map<String, List<UserResource>> resources = new HashMap<>();

    public User(@JsonProperty("userId") String userId, @JsonProperty("realm") String realm) {
        Assert.hasText(userId, "userId can not be null or empty");
        Assert.notNull(realm, "realm can not be null");

        this.userId = userId;
        this.realm = realm;
        this.authorities = Collections.emptySet();
    }

    /*
     * User Resources
     * also unpack/repack by type
     * TODO
     */
    // @JsonAnyGetter
    // public Map<String, List<UserResource>> getResources() {
    //     return resources;
    // }

    // @JsonAnySetter
    // public void setResources(Map<String, List<UserResource>> resources) {
    //     this.resources = resources;
    // }

    // @JsonIgnore
    // public List<UserResource> getResources(String type) {
    //     return resources.get(type);
    // }

    /*
     * Authorities
     *
     */

    public void setAuthorities(Collection<GrantedAuthority> authorities) {
        this.authorities = new HashSet<>();
        if (authorities != null) {
            this.authorities.addAll(authorities);
        }
    }

    /*
     * Resource
     */
    public String getId() {
        return userId;
    }

    public String getType() {
        return SystemKeys.RESOURCE_USER;
    }

    /*
     * Resource context
     */

    @JsonAnyGetter
    @Override
    public Map<String, List<UserResource>> getResources() {
        if (this.resources == null) {
            this.resources = new HashMap<>();
        }

        return resources;
    }

    public void setResources(Map<String, List<UserResource>> resources) {
        this.resources = resources;
    }

    @JsonAnySetter
    public void setResourcesWithType(String type, List<UserResource> resources) {
        if (this.resources == null) {
            this.resources = new HashMap<>();
        }
        this.resources.put(type, resources);
    }

    public static User from(UserDetails details) {
        Assert.notNull(details, "user details can not be null");
        User user = new User(details.getUserId(), details.getRealm());
        user.setUsername(details.getUsername());
        user.setAuthorities(new HashSet<>(details.getAuthorities()));

        return user;
    }
}
