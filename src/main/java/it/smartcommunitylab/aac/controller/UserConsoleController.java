package it.smartcommunitylab.aac.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.MyUserManager;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.profiles.ProfileManager;
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

    @GetMapping("/accounts")
    public ResponseEntity<Page<UserIdentity>> myAccounts(Pageable pageable) throws NoSuchRealmException {
        UserDetails user = currentUser();
        List<UserIdentity> result = new ArrayList<>(user.getIdentities());
        Page<UserIdentity> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/connections")
    public ResponseEntity<Page<ConnectedAppProfile>> myConnections(Pageable pageable) throws NoSuchRealmException {
        UserDetails user = currentUser();
        List<ConnectedAppProfile> result = new ArrayList<>(userManager.getMyConnectedApps());
        Page<ConnectedAppProfile> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDetails> myProfile() throws InvalidDefinitionException {
        UserDetails user = currentUser();

        // TODO use a proper profile for UI (UserProfile..)
        return ResponseEntity.ok(user);

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
