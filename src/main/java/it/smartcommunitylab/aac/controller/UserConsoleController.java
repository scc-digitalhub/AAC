package it.smartcommunitylab.aac.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.MyUserManager;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.ConnectedApp;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.profiles.ProfileManager;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.roles.SpaceRoleManager;
import it.smartcommunitylab.aac.scope.Scope;

@RestController
//@PreAuthorize("hasAuthority('" + Config.R_USER + "')")
@Hidden
@RequestMapping("/console/user")
//@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserConsoleController {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private MyUserManager userManager;

    @Autowired
    private ProfileManager profileManager;

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    private SpaceRoleManager roleManager;

    @Autowired
    private ScopeManager scopeManager;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("request ok");
    }

    /*
     * User
     */
    @GetMapping("/details")
    public ResponseEntity<UserDetails> myUser() throws InvalidDefinitionException {
        UserDetails user = currentUser();

        // TODO use a proper profile for UI (UserProfile..)
        return ResponseEntity.ok(user);

    }

    @DeleteMapping("/details")
    public ResponseEntity<Void> deleteUser() {
        UserDetails user = currentUser();

        userManager.deleteCurUser();
        return ResponseEntity.ok().build();
    }

    /*
     * Accounts
     */
    @GetMapping("/accounts")
    public ResponseEntity<Page<UserIdentity>> listAccounts(Pageable pageable) {
        List<UserIdentity> result = new ArrayList<>(userManager.getMyIdentities());
        Page<UserIdentity> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/accounts/{resourceId}")
    public ResponseEntity<UserIdentity> getAccount(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String resourceId)
            throws NoSuchUserException, NoSuchProviderException {
        UserDetails user = currentUser();

        // explode resourceId
        String[] i = resourceId.split(SystemKeys.ID_SEPARATOR);
        if (i.length != 3) {
            throw new IllegalArgumentException("invalid id format");
        }
        String authority = i[0];
        String provider = i[1];
        String id = i[2];

        UserIdentity identity = userManager.getMyIdentity(authority, provider, id);
        return ResponseEntity.ok(identity);
    }

    @DeleteMapping("/accounts/{resourceId}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String resourceId)
            throws NoSuchUserException, NoSuchProviderException {
        UserDetails user = currentUser();

        // explode resourceId
        String[] i = resourceId.split(SystemKeys.ID_SEPARATOR);
        if (i.length != 3) {
            throw new IllegalArgumentException("invalid id format");
        }
        String authority = i[0];
        String provider = i[1];
        String id = i[2];

        userManager.deleteMyIdentity(authority, provider, id);
        return ResponseEntity.ok().build();
    }

    /*
     * Connections
     */
    @GetMapping("/connections")
    public ResponseEntity<Page<ConnectedApp>> listConnections(Pageable pageable) throws NoSuchRealmException {
        List<ConnectedApp> result = new ArrayList<>(userManager.getMyConnectedApps());
        Page<ConnectedApp> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/connections/{id}")
    public ResponseEntity<ConnectedApp> getConnection(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id)
            throws NoSuchClientException {
        String[] i = id.split(SystemKeys.ID_SEPARATOR);
        if (i.length != 2) {
            throw new IllegalArgumentException("invalid id format");
        }
        String subjectId = i[0];
        String clientId = i[1];

        ConnectedApp app = userManager.getMyConnectedApp(clientId);
        return ResponseEntity.ok(app);
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> deleteConnection(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id)
            throws NoSuchClientException {
        String[] i = id.split(SystemKeys.ID_SEPARATOR);
        if (i.length != 2) {
            throw new IllegalArgumentException("invalid id format");
        }
        String subjectId = i[0];
        String clientId = i[1];

        userManager.deleteMyConnectedApp(clientId);
        return ResponseEntity.ok().build();
    }
    /*
     * Profiles
     */

    @GetMapping("/profile")
    public ResponseEntity<UserDetails> myProfile() throws InvalidDefinitionException {
        UserDetails user = currentUser();

        // TODO use a proper profile for UI (UserProfile..)
        return ResponseEntity.ok(user);

    }

    @GetMapping("/profiles")
    public ResponseEntity<Page<AbstractProfile>> myProfiles(Pageable pageable) throws InvalidDefinitionException {
        List<AbstractProfile> result = new ArrayList<>(userManager.getMyProfiles());
        Page<AbstractProfile> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/audit")
    public ResponseEntity<Page<AuditEvent>> myAudit(Pageable pageable) throws InvalidDefinitionException {
        List<AuditEvent> result = new ArrayList<>(userManager.getMyAuditEvents(null));
        Page<AuditEvent> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/sessions")
    public ResponseEntity<Page<SessionInformation>> mySessions(Pageable pageable) throws InvalidDefinitionException {
        List<SessionInformation> result = new ArrayList<>(userManager.getMySessions());
        Page<SessionInformation> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/scopes")
    public ResponseEntity<Page<Scope>> scopes(Pageable pageable) throws NoSuchRealmException {
        // cleanup scope details

        List<Scope> result = scopeManager.listScopes()
                .stream()
                .filter(s -> ScopeType.CLIENT != s.getType())
                .map(s -> {
                    Scope r = new Scope(s.getScope());
                    r.setName(s.getName());
                    r.setDescription(s.getDescription());
                    r.setResourceId(s.getResourceId());
                    return r;
                })
                .collect(Collectors.toList());
        Page<Scope> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/scopes/{scope}")
    public ResponseEntity<Scope> scope(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String scope)
            throws NoSuchRealmException, NoSuchScopeException {
        Scope s = scopeManager.getScope(scope);
        return ResponseEntity.ok(s);
    }

    private UserDetails currentUser() {
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("Invalid user authentication");
        }

        return user;
    }
}
