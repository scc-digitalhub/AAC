package it.smartcommunitylab.aac.console;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.MyUserManager;
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.core.base.AbstractEditableUserCredentials;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.EditableUserCredentials;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.model.ConnectedApp;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.scope.Scope;

@RestController
@PreAuthorize("hasAuthority('" + Config.R_USER + "')")
@Hidden
@RequestMapping("/console/user")
public class UserConsoleController {

    // TODO remove workaround for token serialization
    private final static ObjectMapper tokenMapper;
    static {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.USE_ANNOTATIONS);
        tokenMapper = mapper;
    }
    private final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private MyUserManager userManager;

    @Autowired
    private ScopeManager scopeManager;

    @GetMapping("/authorities")
    public ResponseEntity<List<? extends GrantedAuthority>> getAuthorities(UserAuthentication auth) {
        return ResponseEntity.ok(new ArrayList<>(auth.getAuthorities()));
    }

    @GetMapping("/status")
    public ResponseEntity<Void> checkStatus() {
        return ResponseEntity.ok().build();
    }

    /*
     * User
     */

    @GetMapping("/details")
    public ResponseEntity<Page<UserDetails>> myUserList(Pageable pageable) throws InvalidDefinitionException {
        UserDetails user = currentUser();
        List<UserDetails> result = Collections.singletonList(user);
        Page<UserDetails> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/details/{userId}")
    public ResponseEntity<UserDetails> myUser() throws InvalidDefinitionException {
        UserDetails user = currentUser();

        // TODO use a proper profile for UI (UserProfile..)
        return ResponseEntity.ok(user);

    }

    @DeleteMapping("/details/{userId}")
    public ResponseEntity<Void> deleteUser() {
        UserDetails user = currentUser();

        userManager.deleteMyUser();
        return ResponseEntity.ok().build();
    }

    /*
     * Accounts
     */
    @GetMapping("/accounts")
    public ResponseEntity<Page<EditableUserAccount>> listAccounts(Pageable pageable)
            throws NoSuchUserException {
        List<EditableUserAccount> result = userManager.getMyAccounts().stream()
                .collect(Collectors.toList());
        Page<EditableUserAccount> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/accounts/{uuid}")
    public ResponseEntity<EditableUserAccount> getAccount(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid)
            throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {

        EditableUserAccount account = userManager.getMyAccount(uuid);
        return ResponseEntity.ok(account);
    }

    @PutMapping("/accounts/{uuid}")
    public ResponseEntity<EditableUserAccount> updateAccount(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid,
            @RequestBody @Valid @NotNull AbstractEditableAccount reg)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {

        EditableUserAccount account = userManager.updateMyAccount(uuid, reg);
        return ResponseEntity.ok(account);
    }

    @DeleteMapping("/accounts/{uuid}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {

        userManager.deleteMyAccount(uuid);
        return ResponseEntity.ok().build();
    }

    /*
     * Credentials
     */
    @GetMapping("/credentials")
    public ResponseEntity<Page<EditableUserCredentials>> listCredentials(Pageable pageable)
            throws NoSuchUserException {
        List<EditableUserCredentials> result = userManager.getMyCredentials().stream()
                .collect(Collectors.toList());
        Page<EditableUserCredentials> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/credentials/{uuid}")
    public ResponseEntity<EditableUserCredentials> getCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid)
            throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {

        EditableUserCredentials cred = userManager.getMyCredentials(uuid);
        return ResponseEntity.ok(cred);
    }

    @PutMapping("/credentials/{uuid}")
    public ResponseEntity<EditableUserCredentials> updateCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid,
            @RequestBody @Valid @NotNull AbstractEditableUserCredentials reg)
            throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException,
            NoSuchAuthorityException {

        EditableUserCredentials cred = userManager.updateMyCredentials(uuid, reg);
        return ResponseEntity.ok(cred);
    }

    @DeleteMapping("/credentials/{uuid}")
    public ResponseEntity<Void> deleteCredentials(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid)
            throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException,
            NoSuchAuthorityException {

        userManager.deleteMyCredentials(uuid);
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

    @GetMapping("/attributes")
    public ResponseEntity<Page<UserAttributes>> myAttributes(Pageable pageable) throws InvalidDefinitionException {
        List<UserAttributes> result = new ArrayList<>(userManager.getMyAttributes());
        Page<UserAttributes> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/audit")
    public ResponseEntity<Page<AuditEvent>> myAudit(Pageable pageable) throws InvalidDefinitionException {
        List<AuditEvent> result = new ArrayList<>(userManager.getMyAuditEvents(null));
        Page<AuditEvent> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/sessions")
    public ResponseEntity<Page<IdentifiableSessionInformation>> mySessions(Pageable pageable)
            throws InvalidDefinitionException {
        List<IdentifiableSessionInformation> result = userManager.getMySessions().stream()
                .map(s -> new IdentifiableSessionInformation(s.getPrincipal(), s.getSessionId(), s.getLastRequest()))
                .collect(Collectors.toList());
        Page<IdentifiableSessionInformation> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/tokens")
    public ResponseEntity<Page<Map<String, Serializable>>> myTokens(Pageable pageable)
            throws InvalidDefinitionException {

        Collection<AACOAuth2AccessToken> tokens = userManager.getMyAccessTokens();
        List<Map<String, Serializable>> result = tokens.stream()
                .map(t -> {
                    HashMap<String, Serializable> m = tokenMapper.convertValue(t, typeRef);
                    m.put("id", t.getToken());
                    return m;
                })
                .filter(t -> t != null).collect(Collectors.toList());
        Page<Map<String, Serializable>> page = new PageImpl<>(result, pageable, result.size());
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

    public static class IdentifiableSessionInformation extends SessionInformation {
        public IdentifiableSessionInformation(Object principal, String sessionId, Date lastRequest) {
            super(principal, sessionId, lastRequest);
        }

        public String getId() {
            return getSessionId();
        }
    }
}
