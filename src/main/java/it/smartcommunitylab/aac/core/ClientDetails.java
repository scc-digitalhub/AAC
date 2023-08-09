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

import it.smartcommunitylab.aac.attributes.model.ClientAttributes;
import it.smartcommunitylab.aac.model.Group;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.SpaceRole;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class ClientDetails {

    // base attributes
    private final String clientId;
    private final String realm;
    private final String type;

    private String name;
    private String description;

    // TODO evaluate move configuration attributes to Client model
    // (client)Details should be used to support authentication inside AAC only

    // providers enabled
    private Collection<String> providers;

    // scopes enabled
    private Collection<String> scopes;
    private Collection<String> resourceIds;

    // hook functions
    private Map<String, String> hookFunctions;
    private Map<String, String> hookWebUrls;
    private String hookUniqueSpaces;

    // TODO evaluate move transient attributes to Client model
    // (client)Details should be used to support authentication inside AAC only

    // attributes related to client
    // sets are bound to realm, stored with addressable keys
    // TODO
    private Map<String, ClientAttributes> attributes;

    // authorities are roles INSIDE aac (ie user/admin/dev etc)
    // we do not want authorities modified inside session
    // note permission checks are performed on authToken authorities, not here
    private final Set<GrantedAuthority> authorities;

    // roles are OUTSIDE aac (ie not grantedAuthorities)
    // roles are associated to CLIENT(=subjectId)
    // this field should be used for caching, consumers should refresh
    // otherwise we should implement an (external) expiring + refreshing cache with
    // locking.

    private Set<RealmRole> realmRoles;
    // this field is always disclosed in cross-realm scenarios
    private Set<SpaceRole> spaceRoles;

    // groups where client is a member, without members list
    private Set<Group> groups;

    // we don't support account enabled/disabled
    // TODO implement as ENUM
    private final boolean enabled;

    public ClientDetails(
        String clientId,
        String realm,
        String type,
        Collection<? extends GrantedAuthority> authorities
    ) {
        Assert.notNull(clientId, "clientId can not be null");
        Assert.notNull(realm, "realm can not be null");
        Assert.notNull(type, "type is required");

        this.clientId = clientId;
        this.realm = realm;
        this.type = type;

        this.authorities = Collections.unmodifiableSet(new HashSet<>(authorities));
        this.enabled = true;

        this.realmRoles = new HashSet<>();
        this.spaceRoles = new HashSet<>();
    }

    public Collection<String> getProviders() {
        return providers;
    }

    public void setProviders(Collection<String> providers) {
        this.providers = providers;
    }

    public Collection<String> getScopes() {
        return scopes;
    }

    public void setScopes(Collection<String> scopes) {
        this.scopes = scopes;
    }

    public Collection<String> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Collection<String> resourceIds) {
        this.resourceIds = resourceIds;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    public Map<String, String> getHookWebUrls() {
        return hookWebUrls;
    }

    public void setHookWebUrls(Map<String, String> hookWebUrls) {
        this.hookWebUrls = hookWebUrls;
    }

    public String getHookUniqueSpaces() {
        return hookUniqueSpaces;
    }

    public void setHookUniqueSpaces(String hookUniqueSpaces) {
        this.hookUniqueSpaces = hookUniqueSpaces;
    }

    public Map<String, ClientAttributes> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, ClientAttributes> attributes) {
        this.attributes = attributes;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRealm() {
        return realm;
    }

    public String getType() {
        return type;
    }

    public Set<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * Roles are mutable and comparable
     */
    public Set<RealmRole> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(Collection<RealmRole> rr) {
        this.realmRoles = new HashSet<>();
        realmRoles.addAll(rr);
    }

    /*
     * Space roles
     */
    public Set<SpaceRole> getSpaceRoles() {
        return spaceRoles;
    }

    public void setSpaceRoles(Collection<SpaceRole> rr) {
        this.spaceRoles = new HashSet<>();
        spaceRoles.addAll(rr);
    }

    /*
     * Groups
     */

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Collection<Group> groups) {
        this.groups = new HashSet<>();
        this.groups.addAll(groups);
    }
}
