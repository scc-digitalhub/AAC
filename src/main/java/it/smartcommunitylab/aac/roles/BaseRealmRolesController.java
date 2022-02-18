package it.smartcommunitylab.aac.roles;

import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchRoleException;
import it.smartcommunitylab.aac.model.RealmRole;

/*
 * Base controller for realm roles
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseRealmRolesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected RealmRoleManager roleManager;

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Realm roles
     */

    @GetMapping("/roles/{realm}")
    public Collection<RealmRole> getRealmRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list roles for realm {}",
                StringUtils.trimAllWhitespace(realm));

        return roleManager.getRealmRoles(realm);
    }

    @PostMapping("/roles/{realm}")
    public RealmRole createRealmRole(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull RealmRole reg)
            throws NoSuchRealmException, NoSuchRoleException {
        logger.debug("add role to realm {}",
                StringUtils.trimAllWhitespace(realm));

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
            logger.trace("role bean: " + StringUtils.trimAllWhitespace(r.toString()));
        }

        r = roleManager.addRealmRole(realm, r);

        return r;
    }

    @GetMapping("/roles/{realm}/{roleId}")
    public RealmRole getRealmRole(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId)
            throws NoSuchRealmException, NoSuchRoleException {
        logger.debug("get role {} for realm {}",
                StringUtils.trimAllWhitespace(roleId), StringUtils.trimAllWhitespace(realm));

        RealmRole r = roleManager.getRealmRole(realm, roleId);

        return r;
    }

    @PutMapping("/roles/{realm}/{roleId}")
    public RealmRole getRealmRole(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
            @RequestBody @Valid @NotNull RealmRole reg)
            throws NoSuchRealmException, NoSuchRoleException {
        logger.debug("update role {} for realm {}",
                StringUtils.trimAllWhitespace(roleId), StringUtils.trimAllWhitespace(realm));

        RealmRole r = roleManager.getRealmRole(realm, roleId);

        // unpack and build model
        String role = reg.getRole();
        String name = reg.getName();
        String description = reg.getDescription();

        r.setRole(role);
        r.setName(name);
        r.setDescription(description);

        if (logger.isTraceEnabled()) {
            logger.trace("role bean: " + StringUtils.trimAllWhitespace(r.toString()));
        }

        r = roleManager.updateRealmRole(realm, roleId, reg);

        return r;
    }

    @DeleteMapping("/roles/{realm}/{roleId}")
    public void removeRealmRole(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId)
            throws NoSuchRealmException, NoSuchRoleException {
        logger.debug("delete role {} for realm {}",
                StringUtils.trimAllWhitespace(roleId), StringUtils.trimAllWhitespace(realm));

        roleManager.deleteRealmRole(realm, roleId);
    }

    /*
     * Scope permissions
     * 
     * TODO add update methods
     */

    @GetMapping("/roles/{realm}/{roleId}/approvals")
    public Collection<Approval> getRealmRoleApprovals(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId)
            throws NoSuchRealmException, NoSuchRoleException {
        Collection<Approval> approvals = roleManager.getRealmRoleApprovals(realm, roleId);
        return approvals;
    }

}
