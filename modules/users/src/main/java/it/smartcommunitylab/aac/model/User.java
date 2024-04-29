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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * A model describing the user outside the auth/security context.
 *
 * In addition to core properties, user resources can be included.
 */
@JsonInclude(Include.NON_NULL)
public class User implements UserResource, UserResourceContext {

    @NotBlank
    private final String userId;

    @NotBlank
    private String realm;

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
    @JsonIgnore
    private Map<String, List<? extends UserResource>> resources = new HashMap<>();

    public User(@JsonProperty("userId") String userId, @JsonProperty("realm") String realm) {
        Assert.hasText(userId, "userId can not be null or empty");
        Assert.notNull(realm, "realm can not be null");

        this.userId = userId;
        this.realm = realm;
        this.authorities = Collections.emptySet();
    }

    public String getUserId() {
        return userId;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
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

    public Boolean getTosAccepted() {
        return tosAccepted;
    }

    public void setTosAccepted(Boolean tosAccepted) {
        this.tosAccepted = tosAccepted;
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

    public Set<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<GrantedAuthority> authorities) {
        this.authorities = new HashSet<>();
        if (authorities != null) {
            this.authorities.addAll(authorities);
        }
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_AAC;
    }

    @Override
    public String getProvider() {
        return SystemKeys.AUTHORITY_AAC;
    }

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_USER;
    }

    /*
     * Resource context
     */

    @JsonAnyGetter
    @Override
    public Map<String, List<? extends UserResource>> getResources() {
        if (this.resources == null) {
            this.resources = new HashMap<>();
        }

        return resources;
    }

    @JsonAnySetter
    public void setResources(Map<String, List<? extends UserResource>> resources) {
        this.resources = resources;
    }

    public static User from(UserDetails details) {
        Assert.notNull(details, "user details can not be null");
        User user = new User(details.getUserId(), details.getRealm());
        user.setUsername(details.getUsername());
        user.setAuthorities(new HashSet<>(details.getAuthorities()));

        return user;
    }
}
