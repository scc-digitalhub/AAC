package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.audit.store.AuditEventStore;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.EditableUserCredentials;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.service.AccountServiceAuthorityService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserAccountService;
import it.smartcommunitylab.aac.core.service.UserCredentialsService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.ConnectedApp;
import it.smartcommunitylab.aac.model.ConnectedDevice;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
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
    private UserAccountService userAccountService;

    @Autowired
    private UserCredentialsService userCredentialsService;

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
    private ExtTokenStore tokenStore;

    @Autowired
    private AuditEventStore auditStore;

    @Autowired
    private ProfileManager profileManager;

    @Autowired
    private AccountServiceAuthorityService accountServiceAuthorityService;

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

    public User getMyUser(String realm) throws NoSuchRealmException {
        Realm r = realmService.getRealm(realm);
        return userService.getUser(curUserDetails(), r.getSlug());
    }

    public void deleteMyUser() {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        try {
            User user = userService.findUser(userId);

            // full delete, need to remove all associated content
            // kill sessions
            sessionManager.destroyUserSessions(userId);

            // approvals
            try {
                Collection<Approval> userApprovals = approvalStore.findUserApprovals(userId);
                approvalStore.revokeApprovals(userApprovals);
                Collection<Approval> clientApprovals = approvalStore.findClientApprovals(userId);
                approvalStore.revokeApprovals(clientApprovals);
            } catch (Exception e) {
            }

            // TODO tokens

            // let userService handle account, registrations etc
            userService.deleteUser(userId);
        } catch (NoSuchUserException e) {
            // nothing more to do
        }
    }

    /*
     * Accounts
     */
    public Collection<EditableUserAccount> getMyAccounts() throws NoSuchUserException {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        return userAccountService.listEditableUserAccounts(userId);
    }

    public EditableUserAccount getMyAccount(String uuid)
            throws NoSuchProviderException, NoSuchUserException, NoSuchAuthorityException {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        // fetch account and check user match
        EditableUserAccount account = userAccountService.getEditableUserAccount(uuid);
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        return account;
    }

    public <U extends EditableUserAccount> EditableUserAccount updateMyAccount(String uuid, U reg)
            throws NoSuchProviderException, NoSuchUserException, RegistrationException, NoSuchAuthorityException {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        // fetch account and check user match
        EditableUserAccount account = userAccountService.getEditableUserAccount(uuid);
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        // execute
        return userAccountService.editUserAccount(uuid, reg);
    }

    public void deleteMyAccount(String uuid)
            throws NoSuchProviderException, NoSuchUserException, RegistrationException, NoSuchAuthorityException {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        // fetch account and check user match
        UserAccount account = userAccountService.getUserAccount(uuid);
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        // execute
        userAccountService.deleteUserAccount(uuid);
    }

    /*
     * Credentials
     */
    public Collection<EditableUserCredentials> getMyCredentials() throws NoSuchUserException {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        return userCredentialsService.listEditableUserCredentials(userId);
    }

    public EditableUserCredentials getMyCredentials(String uuid)
            throws NoSuchCredentialException, NoSuchProviderException, NoSuchUserException, NoSuchAuthorityException {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        // fetch credentials and check user match
        EditableUserCredentials cred = userCredentialsService.getEditableUserCredentials(uuid);
        if (!cred.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        return cred;
    }

    public <U extends EditableUserCredentials> EditableUserCredentials updateMyCredentials(String uuid, U reg)
            throws NoSuchCredentialException, NoSuchProviderException, NoSuchUserException, RegistrationException,
            NoSuchAuthorityException {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        // fetch credentials and check user match
        EditableUserCredentials cred = userCredentialsService.getEditableUserCredentials(uuid);
        if (!cred.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        // execute
        return userCredentialsService.editUserCredentials(uuid, reg);
    }

    public void deleteMyCredentials(String uuid)
            throws NoSuchCredentialException, NoSuchProviderException, NoSuchUserException, RegistrationException,
            NoSuchAuthorityException {
        UserDetails details = curUserDetails();
        String userId = details.getSubjectId();

        // fetch credentials and check user match
        EditableUserCredentials cred = userCredentialsService.getEditableUserCredentials(uuid);
        if (!cred.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        // execute
        userCredentialsService.deleteUserCredentials(uuid);
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
        Map<String, List<Approval>> map = approvals.stream().collect(Collectors.groupingBy(a -> a.getClientId()));
        Set<String> keys = approvals.stream().map(a -> a.getScope()).collect(Collectors.toSet());

        Map<String, Scope> scopes = keys.stream()
                .map(s -> {
                    try {
                        return scopeRegistry.getScope(s);
                    } catch (NoSuchScopeException e) {
                        // client was removed or scope does not exists
                        // we should remove the approval
                        return null;
                    }
                })
                .filter(s -> s != null)
                .collect(Collectors.toMap(s -> s.getScope(), s -> s));

        List<ConnectedApp> apps = map.entrySet().stream()
                .map(e -> {
                    try {
                        String clientId = e.getKey();
                        ClientEntity client = clientService.getClient(clientId);
                        ConnectedApp app = new ConnectedApp(subjectId, client.getClientId(), client.getRealm());
                        app.setAppName(client.getName());
                        app.setAppDescription(client.getDescription());

                        // set approvals
                        app.setApprovals(e.getValue());

                        // evaluate dates
                        Date createDate = e.getValue().stream().map(a -> a.getLastUpdatedAt()).min(Date::compareTo)
                                .orElse(null);
                        Date modifiedDate = e.getValue().stream().map(a -> a.getLastUpdatedAt()).max(Date::compareTo)
                                .orElse(null);
                        Date expireDate = e.getValue().stream().map(a -> a.getExpiresAt()).max(Date::compareTo)
                                .orElse(null);
                        app.setCreateDate(createDate);
                        app.setModifiedDate(modifiedDate);
                        app.setExpireDate(expireDate);

                        List<Scope> list = e.getValue().stream().map(a -> scopes.get(a.getScope()))
                                .filter(s -> s != null)
                                .collect(Collectors.toList());
                        app.setScopes(list);

                        return app;
                    } catch (NoSuchClientException ec) {
                        // client was removed or scope does not exists
                        // we should remove the approval
//                        approvalStore.revokeApprovals(Collections.singleton(appr));
                        return null;
                    }
                })
                .filter(a -> a != null)
                .collect(Collectors.toList());
//
//        for (Approval appr : approvals) {
//            try {
//                String clientId = appr.getClientId();
//                ClientEntity client = clientService.getClient(clientId);
//                Scope scope = scopeRegistry.getScope(appr.getScope());
//
//                if (!map.containsKey(client)) {
//                    map.put(client, new ArrayList<>());
//                }
//
//                map.get(client).add(scope);
//
//            } catch (NoSuchClientException | NoSuchScopeException e) {
//                // client was removed or scope does not exists
//                // we should remove the approval
//                approvalStore.revokeApprovals(Collections.singleton(appr));
//            }
//        }
//
//        List<ConnectedApp> apps = map.entrySet().stream()
//                .map(e -> {
//                    ClientEntity client = e.getKey();
//                    ConnectedApp app = new ConnectedApp(subjectId, client.getClientId(),
//                            client.getRealm(), e.getValue());
//                    app.setAppName(client.getName());
//                    app.setAppDescription(client.getDescription());
//                    return app;
//                })
//                .collect(Collectors.toList());

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

//        // fetch from context for now
//        UserAuthentication userAuth = authHelper.getUserAuthentication();
//        WebAuthenticationDetails webDetails = userAuth.getWebAuthenticationDetails();
//
//        SessionInformation sessionInfo = new SessionInformation(userAuth.getPrincipal(), webDetails.getSessionId(),
//                new Date());
//        return Collections.singleton(sessionInfo);
    }

    /*
     * Tokens
     */
    public Collection<AACOAuth2AccessToken> getMyAccessTokens() {
        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();
        // WORKAROUND: currently tokenStore doesn't persist subject, but only username
        // as such we fetch all matching username, and filter
        String username = details.getUsername();

        Collection<AACOAuth2AccessToken> tokens = tokenStore.findTokensByUserName(username).stream()
                .filter(t -> AACOAuth2AccessToken.class.isInstance(t))
                .map(t -> (AACOAuth2AccessToken) t)
                .filter(t -> subjectId.equals(t.getSubject()))
                .collect(Collectors.toList());

        return tokens;

    }

    // TODO refresh tokens

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
