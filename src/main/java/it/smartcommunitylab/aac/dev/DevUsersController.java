package it.smartcommunitylab.aac.dev;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.roles.SpaceRoleManager;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevUsersController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserManager userManager;

    @Autowired
    private RealmRoleManager roleManager;

    @Autowired
    private SpaceRoleManager spaceRoleManager;

    /*
     * Users
     */
    @GetMapping("/realms/{realm}/users")
    public ResponseEntity<Page<User>> getRealmUsers(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false) String q, Pageable pageRequest) throws NoSuchRealmException {
        return ResponseEntity.ok(userManager.searchUsers(realm, q, pageRequest));
    }

    @GetMapping("/realms/{realm}/users/{subjectId}")
    public ResponseEntity<User> getRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        User user = userManager.getUser(realm, subjectId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/realms/{realm}/users/{subjectId}")
    public ResponseEntity<Void> deleteRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            Authentication authentication)
            throws NoSuchRealmException, NoSuchUserException {
        // check if current user is the same subject
        if (authentication.getName().equals(subjectId)) {
            throw new IllegalArgumentException("Cannot delete current user");
        }
        userManager.removeUser(realm, subjectId);
        return ResponseEntity.ok(null);
    }

    @PutMapping("/realms/{realm}/users/{subjectId}/block")
    public ResponseEntity<User> blockRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        User user = userManager.blockUser(realm, subjectId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/realms/{realm}/users/{subjectId}/block")
    public ResponseEntity<User> unblockRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        User user = userManager.unblockUser(realm, subjectId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/realms/{realm}/users/{subjectId}/lock")
    public ResponseEntity<User> lockRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        User user = userManager.lockUser(realm, subjectId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/realms/{realm}/users/{subjectId}/lock")
    public ResponseEntity<User> unlockRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        User user = userManager.unlockUser(realm, subjectId);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/realms/{realm}/users/invite")
    public ResponseEntity<Void> inviteRealmUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody InvitationBean bean)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException {
        userManager.inviteUser(realm, bean.getUsername(), bean.getSubjectId());
        return ResponseEntity.ok(null);
    }

    /*
     * Roles
     */

    @GetMapping("/realms/{realm}/users/{subjectId}/authorities")
    public ResponseEntity<Collection<GrantedAuthority>> getRealmUserAuthorities(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<GrantedAuthority> authorities = userManager.getAuthorities(realm, subjectId);
        return ResponseEntity.ok(authorities);
    }

    @PutMapping("/realms/{realm}/users/{subjectId}/authorities")
    public ResponseEntity<Collection<GrantedAuthority>> updateRealmUserAuthorities(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @RequestBody Collection<RealmGrantedAuthority> roles) throws NoSuchRealmException, NoSuchUserException {
        // filter roles, make sure they belong to the current realm
        Set<String> values = roles.stream()
                .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
                .map(a -> a.getRole())
                .collect(Collectors.toSet());

        Collection<GrantedAuthority> authorities = userManager.setAuthorities(realm, subjectId,
                values);

        return ResponseEntity.ok(authorities);
    }

    @GetMapping("/realms/{realm}/users/{subjectId}/roles")
    public ResponseEntity<Collection<RealmRole>> getRealmUserRoles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        try {
            Collection<RealmRole> roles = roleManager.getSubjectRoles(realm, subjectId);
            return ResponseEntity.ok(roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    @PutMapping("/realms/{realm}/users/{subjectId}/roles")
    public ResponseEntity<Collection<RealmRole>> updateRealmUserRoles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @RequestBody Collection<RealmRole> roles)
            throws NoSuchRealmException, NoSuchUserException {
        // filter roles, make sure they belong to the current realm
        Set<RealmRole> values = roles.stream()
                .filter(a -> a.getRealm() == null || realm.equals(a.getRealm()))
                .collect(Collectors.toSet());
        try {
            Collection<RealmRole> result = roleManager.setSubjectRoles(realm, subjectId,
                    values);
            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    @GetMapping("/realms/{realm}/users/{subjectId}/spaceroles")
    public ResponseEntity<Collection<SpaceRole>> getRealmUserSpaceRoles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        try {
            Collection<SpaceRole> roles = spaceRoleManager.getRoles(subjectId);
            return ResponseEntity.ok(roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    @PutMapping("/realms/{realm}/users/{subjectId}/spaceroles")
    public ResponseEntity<Collection<SpaceRole>> updateRealmUserSpaceRoles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @RequestBody Collection<String> roles)
            throws NoSuchRealmException, NoSuchUserException {
        try {
            Set<SpaceRole> spaceRoles = roles.stream().map(r -> SpaceRole.parse(r)).collect(Collectors.toSet());
            Collection<SpaceRole> result = spaceRoleManager.setRoles(subjectId, spaceRoles);

            return ResponseEntity.ok(result);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    /*
     * Attributes
     */

    @GetMapping("/realms/{realm}/users/{subjectId}/attributes")
    public ResponseEntity<Collection<UserAttributes>> getRealmUserAttributes(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<UserAttributes> attributes = userManager.getUserAttributes(realm, subjectId);
        return ResponseEntity.ok(attributes);
    }

    @PutMapping("/realms/{realm}/users/{subjectId}/attributes/{provider}/{identifier}")
    public ResponseEntity<UserAttributes> addRealmUserAttributes(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String provider,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identifier,
            @RequestBody Map<String, Serializable> attributes)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {

        // extract registration
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("a valid provider is required");
        }

        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalArgumentException("attributes can not be null");
        }

        // register
        UserAttributes ua = userManager.setUserAttributes(realm, subjectId, provider, identifier, attributes);

        return ResponseEntity.ok(ua);
    }

    @GetMapping("/realms/{realm}/users/{subjectId}/attributes/{provider}/{identifier}")
    public ResponseEntity<UserAttributes> getRealmUserAttributes(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String provider,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identifier)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {
        UserAttributes ua = userManager.getUserAttributes(realm, subjectId, provider, identifier);
        return ResponseEntity.ok(ua);
    }

    @DeleteMapping("/realms/{realm}/users/{subjectId}/attributes/{provider}/{identifier}")
    public ResponseEntity<Void> removeRealmUserAttributes(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String provider,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identifier)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {
        userManager.removeUserAttributes(realm, subjectId, provider, identifier);
        return ResponseEntity.ok(null);
    }

    /*
     * Service approvals (permissions)
     */

    @GetMapping("/realms/{realm}/users/{subjectId}/approvals")
    public ResponseEntity<Collection<Approval>> getRealmUserApprovals(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<Approval> approvals = userManager.getApprovals(realm, subjectId);
        return ResponseEntity.ok(approvals);
    }

    @GetMapping("/realms/{realm}/users/{subjectId}/apps")
    public ResponseEntity<Collection<ConnectedAppProfile>> getRealmUserApps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<ConnectedAppProfile> result = userManager.getConnectedApps(realm, subjectId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/realms/{realm}/users/{subjectId}/apps/{clientId}")
    public ResponseEntity<Void> revokeRealmUserApps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId)
            throws NoSuchRealmException, NoSuchUserException, NoSuchClientException {
        userManager.deleteConnectedApp(realm, subjectId, clientId);
        return ResponseEntity.ok(null);
    }

    /*
     * Audit
     */
    @GetMapping("/realms/{realm}/users/{subjectId}/audit")
    public ResponseEntity<Collection<AuditEvent>> getRealmUserAudit(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @RequestParam(required = false, name = "after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> after,
            @RequestParam(required = false, name = "before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Date> before)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<AuditEvent> result = userManager.getAudit(realm, subjectId, after.orElse(null), before.orElse(null));
        return ResponseEntity.ok(result);
    }

    /*
     * Tokens
     */
    @GetMapping("/realms/{realm}/users/{subjectId}/tokens")
    public ResponseEntity<Collection<OAuth2AccessToken>> getRealmUserTokens(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<OAuth2AccessToken> result = userManager.getAccessTokens(realm, subjectId);
        return ResponseEntity.ok(result);
    }

    /*
     * DTO
     * 
     * TODO cleanup
     */

    public static class InvitationBean {

        private String username, subjectId;

        private List<String> roles;

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(String subjectId) {
            this.subjectId = subjectId;
        }

    }

}
