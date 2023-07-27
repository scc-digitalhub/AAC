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

package it.smartcommunitylab.aac.groups;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.model.Group;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/*
 * Base controller for realm groups
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseGroupController implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected GroupManager groupManager;

    protected RealmRoleManager roleManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(groupManager, "group manager is required");
        Assert.notNull(roleManager, "role manager is required");
    }

    @Autowired
    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Autowired
    public void setRoleManager(RealmRoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Realm groups
     */

    @GetMapping("/groups/{realm}")
    @Operation(summary = "list groups for realm")
    public Collection<Group> getGroups(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm
    ) throws NoSuchRealmException {
        logger.debug("list groups for realm {}", StringUtils.trimAllWhitespace(realm));

        return groupManager.getGroups(realm);
    }

    @PostMapping("/groups/{realm}")
    @Operation(summary = "add a new group for realm")
    public Group createGroup(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull Group reg
    ) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("add role to realm {}", StringUtils.trimAllWhitespace(realm));

        // unpack and build model
        String group = reg.getGroup();
        String parentGroup = reg.getParentGroup();
        String name = reg.getName();
        String description = reg.getDescription();

        Group g = new Group();
        g.setRealm(realm);
        g.setGroup(group);
        g.setParentGroup(parentGroup);
        g.setName(name);
        g.setDescription(description);
        g.setMembers(reg.getMembers());

        if (logger.isTraceEnabled()) {
            logger.trace("group bean: {}", StringUtils.trimAllWhitespace(g.toString()));
        }

        g = groupManager.addGroup(realm, g);
        return g;
    }

    @GetMapping("/groups/{realm}/{groupId}")
    @Operation(summary = "fetch a specific group from realm")
    public Group getGroup(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId
    ) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug(
            "get group {} for realm {}",
            StringUtils.trimAllWhitespace(groupId),
            StringUtils.trimAllWhitespace(realm)
        );

        Group g = groupManager.getGroup(realm, groupId, true);
        return g;
    }

    @PutMapping("/groups/{realm}/{groupId}")
    @Operation(summary = "update a specific group in the realm")
    public Group updateGroup(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId,
        @RequestBody @Valid @NotNull Group reg
    ) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug(
            "update group {} for realm {}",
            StringUtils.trimAllWhitespace(groupId),
            StringUtils.trimAllWhitespace(realm)
        );

        Group g = groupManager.getGroup(realm, groupId, false);

        // unpack and build model
        String group = reg.getGroup();
        String parentGroup = reg.getParentGroup();
        String name = reg.getName();
        String description = reg.getDescription();

        g.setParentGroup(parentGroup);
        g.setName(name);
        g.setDescription(description);
        g.setMembers(reg.getMembers());

        if (logger.isTraceEnabled()) {
            logger.trace("group bean: {}", StringUtils.trimAllWhitespace(g.toString()));
        }

        g = groupManager.updateGroup(realm, groupId, g);

        // enable group rename if requested
        if (!g.getGroup().equals(group)) {
            logger.debug(
                "rename group {} for realm {}",
                StringUtils.trimAllWhitespace(groupId),
                StringUtils.trimAllWhitespace(realm)
            );

            g = groupManager.renameGroup(realm, groupId, group);
        }

        return g;
    }

    @DeleteMapping("/groups/{realm}/{groupId}")
    @Operation(summary = "remove a specific group from realm")
    public void removeGroup(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId
    ) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug(
            "delete group {} for realm {}",
            StringUtils.trimAllWhitespace(groupId),
            StringUtils.trimAllWhitespace(realm)
        );

        groupManager.deleteGroup(realm, groupId);
    }

    /*
     * Group membership
     */

    @GetMapping("/groups/{realm}/{groupId}/members")
    @Operation(summary = "get members for a given group")
    public Collection<String> getGroupMembers(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId
    ) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug(
            "get group {} members for realm {}",
            StringUtils.trimAllWhitespace(groupId),
            StringUtils.trimAllWhitespace(realm)
        );
        Group g = groupManager.getGroup(realm, groupId, false);
        return groupManager.getGroupMembers(realm, g.getGroup());
    }

    @PostMapping("/groups/{realm}/{groupId}/members")
    @Operation(summary = "add subjects as members for a given group")
    public Collection<String> addGroupMembers(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId,
        @RequestBody @Valid @NotNull Collection<String> members
    ) throws NoSuchRealmException, NoSuchGroupException, NoSuchSubjectException {
        logger.debug(
            "add group {} members for realm {}",
            StringUtils.trimAllWhitespace(groupId),
            StringUtils.trimAllWhitespace(realm)
        );
        Group g = groupManager.getGroup(realm, groupId, false);
        if (members != null && !members.isEmpty()) {
            return groupManager.addGroupMembers(realm, g.getGroup(), members);
        }

        return Collections.emptyList();
    }

    @PutMapping("/groups/{realm}/{groupId}/members")
    @Operation(summary = "set subjects as the members for a given group")
    public Collection<String> setGroupMembers(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId,
        @RequestBody @Valid Collection<String> members
    ) throws NoSuchRealmException, NoSuchGroupException, NoSuchSubjectException {
        logger.debug(
            "set group {} members for realm {}",
            StringUtils.trimAllWhitespace(groupId),
            StringUtils.trimAllWhitespace(realm)
        );
        Group g = groupManager.getGroup(realm, groupId, false);
        return groupManager.setGroupMembers(realm, g.getGroup(), members);
    }

    @PutMapping("/groups/{realm}/{groupId}/members/{subjectId}")
    @Operation(summary = "add a specific subject as member for a given group")
    public void addGroupMember(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId
    ) throws NoSuchRealmException, NoSuchGroupException, NoSuchSubjectException {
        logger.debug(
            "add group {} member {} for realm {}",
            StringUtils.trimAllWhitespace(groupId),
            StringUtils.trimAllWhitespace(subjectId),
            StringUtils.trimAllWhitespace(realm)
        );
        Group g = groupManager.getGroup(realm, groupId, false);
        groupManager.addGroupMember(realm, g.getGroup(), subjectId);
    }

    @DeleteMapping("/groups/{realm}/{groupId}/members/{subjectId}")
    @Operation(summary = "remove a specific subject from a given group")
    public void removeGroupMember(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId
    ) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug(
            "delete group {} members {} for realm {}",
            StringUtils.trimAllWhitespace(groupId),
            StringUtils.trimAllWhitespace(subjectId),
            StringUtils.trimAllWhitespace(realm)
        );
        Group g = groupManager.getGroup(realm, groupId, false);
        groupManager.removeGroupMember(realm, g.getGroup(), subjectId);
    }

    /*
     * Roles
     */
    @GetMapping("/groups/{realm}/{groupId}/roles")
    @Operation(summary = "get roles for a specific group")
    public ResponseEntity<Collection<RealmRole>> getGroupRoles(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId
    ) throws NoSuchRealmException, NoSuchGroupException {
        try {
            Collection<RealmRole> roles = roleManager.getSubjectRoles(realm, groupId);
            return ResponseEntity.ok(roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchGroupException();
        }
    }

    @PutMapping("/groups/{realm}/{groupId}/roles")
    @Operation(summary = "set roles for a specific group")
    public ResponseEntity<Collection<RealmRole>> updateGroupRoles(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId,
        @RequestBody @Valid @NotNull Collection<RealmRole> roles
    ) throws NoSuchRealmException, NoSuchGroupException {
        // filter roles, make sure they belong to the current realm
        Set<RealmRole> values = roles
            .stream()
            .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
            .collect(Collectors.toSet());
        try {
            Collection<RealmRole> result = roleManager.setSubjectRoles(realm, groupId, values);
            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchGroupException();
        }
    }
}
