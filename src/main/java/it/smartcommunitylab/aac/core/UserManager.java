package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.Client;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.core.service.UserTranslatorService;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.approval.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

/*
 * Manager for users
 * 
 * should handle all the operations on accounts, by relying on authority managers.
 * Operates on store, so not persisted identities won't be available
 * 
 * Additionally handles operations on the currently logged user, accessed via securityAccessor.
 */

@Service
public class UserManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserService userService;

    @Autowired
    private UserTranslatorService translator;

//    @Autowired
//    private RoleService roleService;

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private SearchableApprovalStore approvalStore;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private RealmService realmService;

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

        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();
        String source = details.getRealm();

        User user = new User(subjectId, details.getRealm());
        for (UserIdentity identity : details.getIdentities()) {
            user.addIdentity(identity);
        }
        for (UserAttributes attr : details.getAttributeSets()) {
            user.addAttributes(attr);
        }

        // TODO
//        user.setAuthorities();
//        user.setRoles(roles);

        if (details.getRealm().equals(realm)) {
            return user;
        } else {
            return translator.translate(user, realm);
        }
    }

    /*
     * Manage users
     */

    /*
     * UserDetails describes user in terms of identities as authorities and
     * authentication
     * 
     * TODO evaluate removal, we should avoid userDetails outside the
     * auth/securityContext
     */
//    public UserDetails getUserDetails(String subjectId, String realm) throws NoSuchUserException {
//        // resolve subject
//        UserEntity user = userService.getUser(subjectId);
//        if (!user.getRealm().equals(realm)) {
//            throw new NoSuchUserException("realm mismatch");
//        }
//
//        // fetch authorities, only for this realm
//        Set<GrantedAuthority> authorities = new HashSet<>();
//        authorities.add(new SimpleGrantedAuthority(Config.R_USER));
//        List<UserRoleEntity> userRoles = userSelrvice.getRoles(subjectId, realm);
//
//        for (UserRoleEntity ur : userRoles) {
//            authorities.add(new RealmGrantedAuthority(ur.getRealm(), ur.getRole()));
//        }
//
//        // fetch identities
//        Set<UserIdentity> identities = new HashSet<>();
//        // TODO we need an order criteria
//        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
//            List<IdentityProvider> idps = ia.getIdentityProviders(realm);
//            for (IdentityProvider idp : idps) {
//                identities.addAll(idp.listIdentities(subjectId));
//            }
//        }
//
//        // fetch attributes outside idps
//        // TODO
//        Set<UserAttributes> attributes = Collections.emptySet();
//
//        // build userDetails
//        return buildUserDetails(subjectId, realm,
//                identities, attributes, authorities);
//
//    }

    /*
     * User describes user in terms of identities as attributes
     */
    // source realm view, complete
    public User getUser(String subjectId) throws NoSuchUserException {
        return userService.getUser(subjectId);
    }

    // per-realm view, partial and translated
    public User getUser(String subjectId, String realm) throws NoSuchUserException, NoSuchRealmException {
        Realm r = realmService.getRealm(realm);

        return userService.getUser(subjectId, realm);
    }

    // per realm view, lists both owned and proxied
    public List<User> listUsers(String realm) throws NoSuchRealmException {
        Realm r = realmService.getRealm(realm);

        return userService.listUsers(realm);
    }

    public void removeUser(String subjectId, String realm) throws NoSuchUserException, NoSuchRealmException {
        Realm r = realmService.getRealm(realm);

        // get user source realm
        String source = userService.getUserRealm(subjectId);
        if (source.equals(realm)) {
            // full delete
            deleteUser(subjectId);
        } else {
            // let userService handle account, registrations etc
            userService.removeUser(subjectId, realm);
        }
    }

    public void deleteUser(String subjectId) throws NoSuchUserException {

        User user = userService.getUser(subjectId);

        // full delete, need to remove all associated content

        // kill sessions
        sessionManager.destroyUserSessions(subjectId);

        // approvals
        try {
            Collection<Approval> approvals = approvalStore.findUserApprovals(subjectId);
            approvalStore.revokeApprovals(approvals);
        } catch (Exception e) {
        }

        // TODO tokens
        // TODO proxy for different realms?

        // let userService handle account, registrations etc
        userService.deleteUser(subjectId);
    }

//    /*
//     * registration
//     */
//    public User registerUser(String providerId, String username, Collection<Map.Entry<String, String>> attributes) {
//        providerManager.getIdentityProvider(providerId);
//    }
//
//    public UserIdentity registerUserIdentity(String providerId, String username,
//            Collection<Map.Entry<String, String>> attributes) {
//
//    }
//
//    /*
//     * credentials
//     */
//    public UserCredentials getUserCredentials(String subjectId, String realm, String userId) {
//
//    }
//
//    public UserCredentials resetUserCredentials();
//
//    public String getResetUserCredentialsLink();
//
//    public UserCredentials setUserCredentials();

    /*
     * 2FA/MFA
     * 
     * TODO add dedicated TOTP/HTOP credentials provider
     */

    /*
     * Attributes (outside accounts)
     * 
     * TODO
     */

    /*
     * Connected apps
     */

    public List<ConnectedAppProfile> getMyConnectedApps() {
        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();

        return getConnectedApps(subjectId);
    }

    public List<ConnectedAppProfile> getConnectedApps(String subjectId) {

        Collection<Approval> approvals = approvalStore.findUserApprovals(subjectId);
        Map<Client, List<Scope>> map = new HashMap<>();

        for (Approval appr : approvals) {
            try {
                String clientId = appr.getClientId();
                Client client = clientManager.getClient(clientId);
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

        List<ConnectedAppProfile> apps = map.entrySet().stream()
                .map(e -> new ConnectedAppProfile(e.getKey().getClientId(), e.getKey().getName(), e.getValue()))
                .collect(Collectors.toList());

        return apps;
    }

    /**
     * @param user
     * @param clientId
     * @return
     */
    public void deleteConnectedApp(String subjectId, String clientId) {

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

}
