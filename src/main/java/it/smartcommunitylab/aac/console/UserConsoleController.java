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

package it.smartcommunitylab.aac.console;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.accounts.model.EditableUserAccount;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
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
import it.smartcommunitylab.aac.core.ScopeManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.model.ConnectedApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.password.model.InternalEditableUserPassword;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.users.MyUserManager;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnEditableUserCredential;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationRequest;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationStartRequest;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnRegistrationRequestStore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('" + Config.R_USER + "')")
@Hidden
@RequestMapping("/console/user")
public class UserConsoleController {

    @Autowired
    private WebAuthnRegistrationRequestStore webAuthnRequestStore;

    // TODO remove workaround for token serialization
    private static final ObjectMapper tokenMapper;

    static {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(MapperFeature.USE_ANNOTATIONS);
        tokenMapper = mapper;
    }

    private final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private MyUserManager userManager;

    @Autowired
    private ScopeManager scopeManager;

    @GetMapping("/me")
    public ResponseEntity<UserDetails> my() throws InvalidDefinitionException {
        UserDetails user = currentUser();

        // TODO use a proper profile for UI (UserProfile..)
        return ResponseEntity.ok(user);
    }

    @GetMapping("/authorities")
    public ResponseEntity<List<? extends GrantedAuthority>> myAuthorities(UserAuthentication auth) {
        return ResponseEntity.ok(new ArrayList<>(auth.getAuthorities()));
    }

    @GetMapping("/status")
    public ResponseEntity<Void> myStatus() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/realm")
    public ResponseEntity<Realm> myRealm() throws NoSuchRealmException {
        Realm realm = userManager.getMyRealm();
        return ResponseEntity.ok(realm);
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
    public ResponseEntity<Page<EditableUserAccount>> listAccounts(Pageable pageable) throws NoSuchUserException {
        List<EditableUserAccount> result = userManager.getMyAccounts().stream().collect(Collectors.toList());
        Page<EditableUserAccount> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/accounts/{uuid}")
    public ResponseEntity<EditableUserAccount> getAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid
    ) throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        EditableUserAccount account = userManager.getMyAccount(uuid);
        return ResponseEntity.ok(account);
    }

    @PutMapping("/accounts/{uuid}")
    public ResponseEntity<EditableUserAccount> updateAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid,
        @RequestBody @Valid @NotNull AbstractEditableAccount reg
    ) throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        EditableUserAccount account = userManager.updateMyAccount(uuid, reg);
        return ResponseEntity.ok(account);
    }

    @DeleteMapping("/accounts/{uuid}")
    public ResponseEntity<Void> deleteAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String uuid
    ) throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        userManager.deleteMyAccount(uuid);
        return ResponseEntity.ok().build();
    }

    /*
     * Credentials: password
     */
    @GetMapping("/password")
    public ResponseEntity<Page<InternalEditableUserPassword>> listPassword(Pageable pageable)
        throws NoSuchUserException {
        List<InternalEditableUserPassword> result = userManager.getMyPassword().stream().collect(Collectors.toList());
        Page<InternalEditableUserPassword> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @PostMapping("/password")
    public ResponseEntity<InternalEditableUserPassword> registerPassword(
        @RequestBody @Valid @NotNull InternalEditableUserPassword reg
    )
        throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        InternalEditableUserPassword cred = userManager.registerMyPassword(reg);
        return ResponseEntity.ok(cred);
    }

    @GetMapping("/password/{id}")
    public ResponseEntity<InternalEditableUserPassword> getPassword(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id
    ) throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        InternalEditableUserPassword cred = userManager.getMyPassword(id);
        return ResponseEntity.ok(cred);
    }

    @PutMapping("/password/{id}")
    public ResponseEntity<InternalEditableUserPassword> updatePassword(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id,
        @RequestBody @Valid @NotNull InternalEditableUserPassword reg
    )
        throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        InternalEditableUserPassword cred = userManager.updateMyPassword(id, reg);
        return ResponseEntity.ok(cred);
    }

    @DeleteMapping("/password/{id}")
    public ResponseEntity<Void> deletePassword(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id
    )
        throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        userManager.deleteMyPassword(id);
        return ResponseEntity.ok().build();
    }

    /*
     * Credentials: webauthn
     */
    @GetMapping("/webauthn")
    public ResponseEntity<Page<WebAuthnEditableUserCredential>> listWebAuthnCredentials(Pageable pageable)
        throws NoSuchUserException {
        List<WebAuthnEditableUserCredential> result = userManager
            .getMyWebAuthnCredentials()
            .stream()
            .collect(Collectors.toList());
        Page<WebAuthnEditableUserCredential> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/webauthn/{id}")
    public ResponseEntity<WebAuthnEditableUserCredential> getWebAuthnCredentials(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id
    ) throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        WebAuthnEditableUserCredential cred = userManager.getMyWebAuthnCredential(id);
        return ResponseEntity.ok(cred);
    }

    @PutMapping("/webauthn/{id}")
    public ResponseEntity<WebAuthnEditableUserCredential> updateWebAuthnCredentials(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id,
        @RequestBody @Valid @NotNull WebAuthnEditableUserCredential reg
    )
        throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        WebAuthnEditableUserCredential cred = userManager.updateMyWebAuthnCredential(id, reg);
        return ResponseEntity.ok(cred);
    }

    @DeleteMapping("/webauthn/{id}")
    public ResponseEntity<Void> deleteCredentials(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id
    )
        throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        userManager.deleteMyWebAuthnCredential(id);
        return ResponseEntity.ok().build();
    }

    // webauthn registration 2 step flow
    @RequestMapping(method = RequestMethod.PATCH, value = "/webauthn")
    public ResponseEntity<WebAuthnRegistrationResponse> attestateWebAuthnCredential(
        @RequestBody(required = false) @Nullable WebAuthnEditableUserCredential reg
    )
        throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException, JsonProcessingException {
        WebAuthnRegistrationRequest request = userManager.registerMyWebAuthnCredential(reg);

        // store request
        String key = webAuthnRequestStore.store(request);

        //rebuild key to export for client
        PublicKeyCredentialCreationOptions publicKey = PublicKeyCredentialCreationOptions.fromJson(
            request.getCredentialCreationInfo().getOptions()
        );

        // build response
        WebAuthnRegistrationResponse response = new WebAuthnRegistrationResponse(
            key,
            publicKey.toCredentialsCreateJson()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/webauthn")
    public ResponseEntity<WebAuthnEditableUserCredential> registerWebAuthnCredential(
        @RequestBody @NotNull WebAuthnEditableUserCredential reg
    )
        throws NoSuchCredentialException, NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        // we skip validation so we check here
        String key = reg.getKey();
        if (!StringUtils.hasText(key)) {
            throw new RegistrationException("invalid key");
        }

        if (!StringUtils.hasText(reg.getAttestation())) {
            throw new RegistrationException("invalid attestation");
        }

        // fetch registration
        WebAuthnRegistrationRequest request = webAuthnRequestStore.consume(key);
        if (request == null) {
            // no registration in progress with this key
            throw new RegistrationException("invalid key");
        }

        WebAuthnEditableUserCredential cred = userManager.registerMyWebAuthnCredential(request, reg);
        return ResponseEntity.ok(cred);
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
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id
    ) throws NoSuchClientException {
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
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String id
    ) throws NoSuchClientException {
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
        List<IdentifiableSessionInformation> result = userManager
            .getMySessions()
            .stream()
            .map(s -> new IdentifiableSessionInformation(s.getPrincipal(), s.getSessionId(), s.getLastRequest()))
            .collect(Collectors.toList());
        Page<IdentifiableSessionInformation> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/tokens")
    public ResponseEntity<Page<Map<String, Serializable>>> myTokens(Pageable pageable)
        throws InvalidDefinitionException {
        Collection<AACOAuth2AccessToken> tokens = userManager.getMyAccessTokens();
        List<Map<String, Serializable>> result = tokens
            .stream()
            .map(t -> {
                HashMap<String, Serializable> m = tokenMapper.convertValue(t, typeRef);
                m.put("id", t.getToken());
                return m;
            })
            .filter(t -> t != null)
            .collect(Collectors.toList());
        Page<Map<String, Serializable>> page = new PageImpl<>(result, pageable, result.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/scopes")
    public ResponseEntity<Page<Scope>> scopes(Pageable pageable) throws NoSuchRealmException {
        // cleanup scope details

        List<Scope> result = scopeManager
            .listScopes()
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
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String scope
    ) throws NoSuchRealmException, NoSuchScopeException {
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
