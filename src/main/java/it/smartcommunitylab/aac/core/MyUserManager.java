package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.audit.RealmAuditEvent;
import it.smartcommunitylab.aac.audit.store.AuditEventStore;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.ConnectedApp;
import it.smartcommunitylab.aac.model.ConnectedDevice;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.profiles.ProfileManager;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.EmailProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

/*
 * Manager for current user
 * 
 * should handle operations on the currently logged user *only*
 */

@Service
public class MyUserManager {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserEntityService userEntityService;

    @Autowired
    private RealmService realmService;

    @Autowired
    private SearchableApprovalStore approvalStore;

    @Autowired
    private ClientEntityService clientService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private AuditEventStore auditStore;

    @Autowired
    private ProfileManager profileManager;

    @Autowired
    private AuthorityManager authorityManager;

    /*
     * Current user, from context
     */

    public UserDetails curUserDetails() {
        UserDetails details = authHelper.getUserDetails();
        if (details == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }

        return details;
    }

    /*
     * Returns a model describing the current user as accessible for the given
     * realm.
     * 
     * For same-realm scenarios the model will be complete, while on cross-realm
     * some fields should be removed or empty.
     */
    public User curUser(String realm) throws NoSuchRealmException {
        Realm r = realmService.getRealm(realm);
        return userService.getUser(curUserDetails(), r.getSlug());
    }

    public void deleteCurUser() {
        UserDetails details = authHelper.getUserDetails();
        if (details == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }

        String subjectId = details.getSubjectId();
        try {
            User user = userService.findUser(subjectId);

            // full delete, need to remove all associated content
            // kill sessions
            sessionManager.destroyUserSessions(subjectId);

            // approvals
            try {
                Collection<Approval> userApprovals = approvalStore.findUserApprovals(subjectId);
                approvalStore.revokeApprovals(userApprovals);
                Collection<Approval> clientApprovals = approvalStore.findClientApprovals(subjectId);
                approvalStore.revokeApprovals(clientApprovals);
            } catch (Exception e) {
            }

            // TODO tokens
            // TODO proxy for different realms?

            // let userService handle account, registrations etc
            userService.deleteUser(subjectId);
        } catch (NoSuchUserException e) {
            // nothing more to do
        }
    }

    /*
     * Accounts
     */
    public Collection<UserIdentity> getMyIdentities() {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();
        String realm = details.getRealm();

        Map<String, UserIdentity> identities = new HashMap<>();
        details.getIdentities().forEach(i -> identities.put(i.getUuid(), i));

        // get all active idp and fetch identities not in session
        Collection<IdentityProvider<? extends UserIdentity>> providers = authorityManager.fetchIdentityProviders(realm);
        List<UserIdentity> ids = providers.stream().flatMap(idp -> idp.listIdentities(userId).stream())
                .collect(Collectors.toList());
        ids.forEach(i -> identities.putIfAbsent(i.getUuid(), i));

        // make sure credentials are erased
        identities.forEach((key, identity) -> {
            if (identity instanceof CredentialsContainer) {
                ((CredentialsContainer) identity).eraseCredentials();
            }
        });

        return identities.values();
    }

    public UserIdentity getMyIdentity(String authority, String providerId, String id)
            throws NoSuchProviderException, NoSuchUserException {
        UserDetails details = curUserDetails();
        IdentityProvider<? extends UserIdentity> idp = authorityManager.fetchIdentityProvider(authority, providerId);
        if (idp == null) {
            throw new NoSuchProviderException();
        }

        UserIdentity identity = idp.getIdentity(id, true);
        if (!details.getSubjectId().equals(identity.getUserId())) {
            // user mismatch
            throw new NoSuchUserException();
        }

        // make sure credentials are erased
        if (identity instanceof CredentialsContainer) {
            ((CredentialsContainer) identity).eraseCredentials();
        }

        return identity;
    }

    public <U extends UserAccount> UserIdentity updateMyIdentity(String authority, String providerId, String id,
            U account,
            Collection<UserAttributes> attributes)
            throws NoSuchProviderException, NoSuchUserException {
        UserDetails details = curUserDetails();
        IdentityService<? extends UserIdentity, ? extends UserAccount> idp = authorityManager
                .fetchIdentityService(authority, providerId);
        if (idp == null) {
            throw new NoSuchProviderException();
        }

        UserIdentity identity = idp.getIdentity(id, true);
        if (!details.getSubjectId().equals(identity.getUserId())) {
            // user mismatch
            throw new NoSuchUserException();
        }

        // update
        identity = idp.updateIdentity(id, account, attributes);

        // make sure credentials are erased
        if (identity instanceof CredentialsContainer) {
            ((CredentialsContainer) identity).eraseCredentials();
        }

        return identity;
    }

    public void deleteMyIdentity(String authority, String providerId, String id)
            throws NoSuchProviderException, NoSuchUserException {
        UserDetails details = curUserDetails();
        IdentityProvider<? extends UserIdentity> idp = authorityManager.fetchIdentityProvider(authority, providerId);
        if (idp == null) {
            throw new NoSuchProviderException();
        }

        UserIdentity identity = idp.getIdentity(id, true);
        if (!details.getSubjectId().equals(identity.getUserId())) {
            // user mismatch
            throw new NoSuchUserException();
        }

        idp.deleteIdentity(id);
    }

    /*
     * Attributes (inside + outside accounts)
     * 
     * TODO
     */

    public Collection<UserAttributes> getMyAttributes() {
        return getMyAttributes(true);
    }

    public Collection<UserAttributes> getMyAttributes(boolean includeIdentities) {
        UserDetails details = curUserDetails();
        Set<UserAttributes> attributes = new HashSet<>();

        // add all attributes from context
        attributes.addAll(details.getAttributeSets(!includeIdentities));

        // refresh attributes from additional providers
        // TODO

        return attributes;
    }
//    public Collection<UserAttributes> getUserAttributes(String realm, String subjectId) throws NoSuchUserException {
//        // TODO should invoke attributeManager
//        return null;
//    }

    /*
     * Connected apps: current user
     */

    @Transactional(readOnly = false)
    public Collection<ConnectedApp> getMyConnectedApps() {
        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();
        Collection<Approval> approvals = approvalStore.findUserApprovals(subjectId);
        Map<ClientEntity, List<Scope>> map = new HashMap<>();

        for (Approval appr : approvals) {
            try {
                String clientId = appr.getClientId();
                ClientEntity client = clientService.getClient(clientId);
                Scope scope = scopeRegistry.getScope(appr.getScope());

                if (!map.containsKey(client)) {
                    map.put(client, new ArrayList<>());
                }

                map.get(client).add(scope);

            } catch (NoSuchClientException | NoSuchScopeException e) {
                // client was removed or scope does not exists
                // we should remove the approval
                approvalStore.revokeApprovals(Collections.singleton(appr));
            }
        }

        List<ConnectedApp> apps = map.entrySet().stream()
                .map(e -> {
                    ClientEntity client = e.getKey();
                    ConnectedApp app = new ConnectedApp(subjectId, client.getClientId(),
                            client.getRealm(), e.getValue());
                    app.setAppName(client.getName());
                    app.setAppDescription(client.getDescription());
                    return app;
                })
                .collect(Collectors.toList());

        return apps;
    }

    @Transactional(readOnly = false)
    public ConnectedApp getMyConnectedApp(String clientId) throws NoSuchClientException {
        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();

        ClientEntity client = clientService.getClient(clientId);
        Collection<Approval> approvals = approvalStore.getApprovals(subjectId, clientId);
        List<Scope> scopes = approvals.stream().map(a -> scopeRegistry.findScope(a.getScope())).filter(s -> s != null)
                .collect(Collectors.toList());

        ConnectedApp app = new ConnectedApp(subjectId, client.getClientId(),
                client.getRealm(), scopes);
        app.setAppName(client.getName());
        app.setAppDescription(client.getDescription());

        return app;
    }

    public void deleteMyConnectedApp(String clientId) {
        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();

        // TODO revoke tokens

//        Collection<OAuth2AccessToken> tokens = tokenStore.findTokensByClientIdAndUserName(clientId, user.toString());
//        for (OAuth2AccessToken token : tokens) {
//            if (token.getRefreshToken() != null) {
//                // remove refresh token
//                OAuth2RefreshToken refreshToken = token.getRefreshToken();
//                tokenStore.removeRefreshToken(refreshToken);
//            }
//
//            // remove access token
//            tokenStore.removeAccessToken(token);
//        }

        // remove approvals
        Collection<Approval> approvals = approvalStore.getApprovals(subjectId, clientId);
        if (!approvals.isEmpty()) {
            approvalStore.revokeApprovals(approvals);
        }

    }

    /*
     * Providers
     */

    @Deprecated
    public Collection<ConfigurableIdentityProvider> getMyIdentityProviders()
            throws NoSuchRealmException, NoSuchUserException {
        UserDetails details = curUserDetails();
        String realm = details.getRealm();
        // filter per user
        Set<String> idps = details.getIdentities().stream().map(i -> i.getProvider()).collect(Collectors.toSet());
        return identityProviderService.listProviders(realm).stream()
                .filter(cp -> idps.contains(cp.getProvider()))
                .map(cp -> {
                    // clear config and reserved info
                    cp.setEvents(null);
                    cp.setPersistence(null);
                    cp.setSchema(null);
                    cp.setConfiguration(null);
                    cp.setHookFunctions(null);

                    return cp;
                }).collect(Collectors.toList());

    }

    /*
     * Profiles
     * 
     * TODO
     */
    public Collection<AbstractProfile> getMyProfiles() {
        UserDetails details = curUserDetails();
        String realm = details.getRealm();
        String subjectId = details.getSubjectId();
        Collection<AbstractProfile> profiles = new ArrayList<>();

        try {
            // add basic
            profiles.add(profileManager.getProfile(realm, subjectId, BasicProfile.IDENTIFIER));
            // add openid
            profiles.add(profileManager.getProfile(realm, subjectId, OpenIdProfile.IDENTIFIER));
            // add email
            profiles.add(profileManager.getProfile(realm, subjectId, EmailProfile.IDENTIFIER));

        } catch (NoSuchUserException | InvalidDefinitionException e) {

        }

        return profiles;

    }

    /*
     * Devices
     * 
     * TODO
     */
    public Collection<ConnectedDevice> getMyDevices() {
        return Collections.emptyList();
    }

    /*
     * Sessions
     * 
     * TODO
     */
    public Collection<SessionInformation> getMySessions() {
        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();

        return sessionManager.listUserSessions(subjectId, details.getRealm(), details.getUsername());
    }

    /*
     * Audit
     * 
     * TODO
     */
    public Collection<AuditEvent> getMyAuditEvents(
            String type) {
        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();

        return auditStore.findByPrincipal(subjectId, null, null, type);

    }
}
