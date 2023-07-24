package it.smartcommunitylab.aac.roles;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchRoleException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.model.RealmRole;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * Base controller for realm roles
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseRealmRolesController implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected RealmRoleManager roleManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(roleManager, "role manager is required");
    }

    @Autowired
    public void setRoleManager(RealmRoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Realm roles
     */

    @GetMapping("/roles/{realm}")
    @Operation(summary = "list roles for realm")
    public Collection<RealmRole> getRealmRoles(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm
    ) throws NoSuchRealmException {
        logger.debug("list roles for realm {}", StringUtils.trimAllWhitespace(realm));

        return roleManager.getRealmRoles(realm);
    }

    @PostMapping("/roles/{realm}")
    @Operation(summary = "add a new role for realm")
    public RealmRole createRealmRole(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull RealmRole reg
    ) throws NoSuchRealmException, NoSuchRoleException {
        logger.debug("add role to realm {}", StringUtils.trimAllWhitespace(realm));

        // unpack and build model
        String role = reg.getRole();
        String name = reg.getName();
        String description = reg.getDescription();

        RealmRole r = new RealmRole();
        r.setRealm(realm);
        r.setRole(role);
        r.setName(name);
        r.setDescription(description);

        if (logger.isTraceEnabled()) {
            logger.trace("role bean: {}", StringUtils.trimAllWhitespace(r.toString()));
        }

        r = roleManager.addRealmRole(realm, r);

        return r;
    }

    @GetMapping("/roles/{realm}/{roleId}")
    @Operation(summary = "fetch a specific role from realm")
    public RealmRole getRealmRole(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId
    ) throws NoSuchRealmException, NoSuchRoleException {
        logger.debug(
            "get role {} for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );

        RealmRole r = roleManager.getRealmRole(realm, roleId, true);

        return r;
    }

    @PutMapping("/roles/{realm}/{roleId}")
    @Operation(summary = "update a specific role in the realm")
    public RealmRole updateRealmRole(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @RequestBody @Valid @NotNull RealmRole reg
    ) throws NoSuchRealmException, NoSuchRoleException {
        logger.debug(
            "update role {} for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );

        RealmRole r = roleManager.getRealmRole(realm, roleId, false);

        // unpack and build model
        String role = reg.getRole();
        String name = reg.getName();
        String description = reg.getDescription();

        r.setRole(role);
        r.setName(name);
        r.setDescription(description);

        if (logger.isTraceEnabled()) {
            logger.trace("role bean: {}", StringUtils.trimAllWhitespace(r.toString()));
        }

        r = roleManager.updateRealmRole(realm, roleId, reg);

        return r;
    }

    @DeleteMapping("/roles/{realm}/{roleId}")
    @Operation(summary = "remove a specific role from realm")
    public void removeRealmRole(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId
    ) throws NoSuchRealmException, NoSuchRoleException {
        logger.debug(
            "delete role {} for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );

        roleManager.deleteRealmRole(realm, roleId);
    }

    /*
     * Scope permissions
     */

    @GetMapping("/roles/{realm}/{roleId}/approvals")
    @Operation(summary = "get approvals for a given role")
    public Collection<Approval> getRealmRoleApprovals(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId
    ) throws NoSuchRealmException, NoSuchRoleException {
        Collection<Approval> approvals = roleManager.getRealmRoleApprovals(realm, roleId);
        return approvals;
    }

    @PostMapping("/roles/{realm}/{roleId}/approvals")
    @Operation(summary = "add approvals to a given role")
    public Collection<Approval> addRealmRoleApprovals(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @RequestParam(required = false, defaultValue = "true") boolean approved,
        @RequestBody @Valid @NotNull Collection<String> scopes
    ) throws NoSuchRealmException, NoSuchRoleException, NoSuchScopeException {
        logger.debug(
            "add approvals to role {} realm {} for scope {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(String.valueOf(scopes))
        );

        Map<String, Boolean> map = scopes.stream().collect(Collectors.toMap(s -> s, s -> approved));
        return roleManager.setRealmRoleApprovals(realm, roleId, map);
    }

    @PutMapping("/roles/{realm}/{roleId}/approvals")
    @Operation(summary = "set approvals for a given role")
    public Collection<Approval> updateRealmRoleApprovals(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @RequestParam(required = false, defaultValue = "true") boolean approved,
        @RequestBody @Valid @NotNull Collection<String> scopes
    ) throws NoSuchRealmException, NoSuchRoleException, NoSuchScopeException {
        logger.debug(
            "add approvals to role {} realm {} for scope {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(String.valueOf(scopes))
        );

        Map<String, Boolean> map = scopes.stream().collect(Collectors.toMap(s -> s, s -> approved));
        return roleManager.addRealmRoleApprovals(realm, roleId, map);
    }

    @PutMapping("/roles/{realm}/{roleId}/approvals/{scope}")
    @Operation(summary = "add a specific permission to a given role")
    public Approval addRealmRoleApproval(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
        @RequestParam(required = false, defaultValue = "true") boolean approved
    ) throws NoSuchRealmException, NoSuchRoleException, NoSuchScopeException {
        logger.debug(
            "add approval for scope {} role {} for realm {}",
            StringUtils.trimAllWhitespace(scope),
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );
        return roleManager.addRealmRoleApproval(realm, roleId, scope, approved);
    }

    @DeleteMapping("/roles/{realm}/{roleId}/approvals/{scope}")
    @Operation(summary = "remove a specific permission from a given role")
    public void deleteRealmRoleApprovals(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SCOPE_PATTERN) String scope,
        @RequestParam @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId
    ) throws NoSuchRealmException, NoSuchScopeException, NoSuchRoleException {
        logger.debug(
            "revoke approval for scope {} role {} for realm {}",
            StringUtils.trimAllWhitespace(scope),
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );
        roleManager.removeRealmRoleApproval(realm, roleId, scope);
    }

    /*
     * Role assignment
     */

    @GetMapping("/roles/{realm}/{roleId}/subjects")
    @Operation(summary = "get subjects for a given role")
    public Collection<String> getRoleSubjects(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId
    ) throws NoSuchRealmException, NoSuchRoleException {
        logger.debug(
            "get role {} subjects for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );
        RealmRole r = roleManager.getRealmRole(realm, roleId);
        return roleManager.getRoleSubjects(realm, r.getRole());
    }

    @PostMapping("/roles/{realm}/{roleId}/subjects")
    @Operation(summary = "add subjects a given role")
    public Collection<String> addRoleSubjects(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @RequestBody @Valid @NotNull Collection<String> subjects
    ) throws NoSuchRealmException, NoSuchRoleException, NoSuchSubjectException {
        logger.debug(
            "add role {} subjects for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );
        RealmRole r = roleManager.getRealmRole(realm, roleId);
        if (subjects != null && !subjects.isEmpty()) {
            return roleManager.addRoleSubjects(realm, r.getRole(), subjects);
        }

        return Collections.emptyList();
    }

    @PutMapping("/roles/{realm}/{roleId}/subjects")
    @Operation(summary = "set subjects as the assignee for a given role")
    public Collection<String> setRoleSubjects(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @RequestBody @Valid Collection<String> members
    ) throws NoSuchRealmException, NoSuchRoleException, NoSuchSubjectException {
        logger.debug(
            "set role {} subjects for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );
        RealmRole r = roleManager.getRealmRole(realm, roleId);
        return roleManager.setRoleSubjects(realm, r.getRole(), members);
    }

    @PutMapping("/roles/{realm}/{roleId}/subjects/{subjectId}")
    @Operation(summary = "add a specific subject a given role")
    public void addRoleSubject(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId
    ) throws NoSuchRealmException, NoSuchRoleException, NoSuchSubjectException {
        logger.debug(
            "add role {} subject {} for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(subjectId),
            StringUtils.trimAllWhitespace(realm)
        );
        RealmRole r = roleManager.getRealmRole(realm, roleId);
        roleManager.addRoleSubject(realm, r.getRole(), subjectId);
    }

    @DeleteMapping("/roles/{realm}/{roleId}/subjects/{subjectId}")
    @Operation(summary = "remove for a specific subject a given role")
    public void removeRoleSubject(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId
    ) throws NoSuchRealmException, NoSuchRoleException {
        logger.debug(
            "delete role {} subject {} for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(subjectId),
            StringUtils.trimAllWhitespace(realm)
        );
        RealmRole r = roleManager.getRealmRole(realm, roleId);
        roleManager.removeRoleSubject(realm, r.getRole(), subjectId);
    }
}
