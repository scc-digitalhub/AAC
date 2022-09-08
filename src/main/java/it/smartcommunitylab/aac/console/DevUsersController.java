package it.smartcommunitylab.aac.console;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.controller.BaseUserController;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.groups.GroupManager;
import it.smartcommunitylab.aac.model.Group;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.roles.SpaceRoleManager;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevUsersController extends BaseUserController {

    @Autowired
    protected GroupManager groupManager;

    @Autowired
    private RealmRoleManager roleManager;

    @Autowired
    private SpaceRoleManager spaceRoleManager;

    /*
     * Groups
     */
    @GetMapping("/users/{realm}/{userId}/groups")
    public ResponseEntity<Collection<Group>> getUserGroups(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        try {
            Collection<Group> groups = groupManager.getSubjectGroups(realm, userId);
            return ResponseEntity.ok(groups);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    @PutMapping("/users/{realm}/{userId}/groups")
    public ResponseEntity<Collection<Group>> updateUserGroups(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid @NotNull Collection<Group> groups)
            throws NoSuchRealmException, NoSuchUserException, NoSuchGroupException {
        // filter groups, make sure they belong to the current realm
        Set<Group> values = groups.stream()
                .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
                .collect(Collectors.toSet());
        try {
            Collection<Group> result = groupManager.setSubjectGroups(realm, userId,
                    values);
            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    /*
     * Roles
     */

    @GetMapping("/users/{realm}/{userId}/authorities")
    public ResponseEntity<Collection<GrantedAuthority>> getUserAuthorities(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<GrantedAuthority> authorities = userManager.getAuthorities(realm, userId);
        return ResponseEntity.ok(authorities);
    }

    @PutMapping("/users/{realm}/{userId}/authorities")
    public ResponseEntity<Collection<GrantedAuthority>> updateUserAuthorities(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid @NotNull Collection<RealmGrantedAuthority> roles)
            throws NoSuchRealmException, NoSuchUserException {
        // filter roles, make sure they belong to the current realm
        Set<String> values = roles.stream()
                .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
                .map(a -> a.getRole())
                .collect(Collectors.toSet());

        Collection<GrantedAuthority> authorities = userManager.setAuthorities(realm, userId,
                values);

        return ResponseEntity.ok(authorities);
    }

    @GetMapping("/users/{realm}/{userId}/roles")
    public ResponseEntity<Collection<RealmRole>> getUserRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<RealmRole> roles = userManager.getUserRealmRoles(realm, userId);
        return ResponseEntity.ok(roles);
    }

    @PutMapping("/users/{realm}/{userId}/roles")
    public ResponseEntity<Collection<RealmRole>> updateUserRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid @NotNull Collection<RealmRole> roles)
            throws NoSuchRealmException, NoSuchUserException {
        // filter roles, make sure they belong to the current realm
        Set<RealmRole> values = roles.stream()
                .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
                .collect(Collectors.toSet());
        try {
            Collection<RealmRole> result = roleManager.setSubjectRoles(realm, userId,
                    values);
            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    @GetMapping("/users/{realm}/{userId}/spaceroles")
    public ResponseEntity<Collection<SpaceRole>> getUserSpaceRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        try {
            Collection<SpaceRole> roles = spaceRoleManager.getRoles(userId);
            return ResponseEntity.ok(roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    @PutMapping("/users/{realm}/{userId}/spaceroles")
    public ResponseEntity<Collection<SpaceRole>> updateUserSpaceRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid @NotNull Collection<String> roles)
            throws NoSuchRealmException, NoSuchUserException {
        try {
            Set<SpaceRole> spaceRoles = roles.stream().map(r -> SpaceRole.parse(r)).collect(Collectors.toSet());
            Collection<SpaceRole> result = spaceRoleManager.setRoles(userId, spaceRoles);

            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    /*
     * Attributes
     */

    @GetMapping("/users/{realm}/{userId}/attributes")
    public ResponseEntity<Collection<UserAttributes>> getRealmUserAttributes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<UserAttributes> attributes = userManager.getUserAttributes(realm, userId);
        return ResponseEntity.ok(attributes);
    }

    @PutMapping("/users/{realm}/{userId}/attributes/{provider}/{identifier}")
    public ResponseEntity<UserAttributes> addRealmUserAttributes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String provider,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identifier,
            @RequestBody @Valid @NotNull Map<String, Serializable> attributes)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {

        // extract registration
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("a valid provider is required");
        }

        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalArgumentException("attributes can not be null");
        }

        // register
        UserAttributes ua = userManager.setUserAttributes(realm, userId, provider, identifier, attributes);

        return ResponseEntity.ok(ua);
    }

    @GetMapping("/users/{realm}/{userId}/attributes/{provider}/{identifier}")
    public ResponseEntity<UserAttributes> getRealmUserAttributes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String provider,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identifier)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {
        UserAttributes ua = userManager.getUserAttributes(realm, userId, provider, identifier);
        return ResponseEntity.ok(ua);
    }

    @DeleteMapping("/users/{realm}/{userId}/attributes/{provider}/{identifier}")
    public ResponseEntity<Void> removeRealmUserAttributes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String provider,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identifier)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {
        userManager.removeUserAttributes(realm, userId, provider, identifier);
        return ResponseEntity.ok(null);
    }

    /*
     * Service approvals (permissions)
     */

    @GetMapping("/users/{realm}/{userId}/approvals")
    public ResponseEntity<Collection<Approval>> getRealmUserApprovals(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<Approval> approvals = userManager.getApprovals(realm, userId);
        return ResponseEntity.ok(approvals);
    }

    @GetMapping("/users/{realm}/{userId}/apps")
    public ResponseEntity<Collection<ConnectedAppProfile>> getRealmUserApps(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<ConnectedAppProfile> result = userManager.getConnectedApps(realm, userId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/users/{realm}/{userId}/apps/{clientId}")
    public ResponseEntity<Void> revokeRealmUserApps(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchUserException, NoSuchClientException {
        userManager.deleteConnectedApp(realm, userId, clientId);
        return ResponseEntity.ok(null);
    }

    /*
     * Audit
     */
    @GetMapping("/users/{realm}/{userId}/audit")
    public ResponseEntity<Collection<AuditEvent>> getRealmUserAudit(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestParam(required = false, name = "after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> after,
            @RequestParam(required = false, name = "before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> before)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<AuditEvent> result = userManager.getAudit(realm, userId, after.orElse(null), before.orElse(null));
        return ResponseEntity.ok(result);
    }

    /*
     * Tokens
     */
    @GetMapping("/users/{realm}/{userId}/tokens")
    public ResponseEntity<Collection<OAuth2AccessToken>> getRealmUserTokens(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<OAuth2AccessToken> result = userManager.getAccessTokens(realm, userId);
        return ResponseEntity.ok(result);
    }

}
