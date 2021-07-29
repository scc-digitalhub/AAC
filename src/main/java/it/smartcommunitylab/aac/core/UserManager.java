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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

/*
 * Manager for users
 * 
 * should handle all the operations on accounts, by relying on authority managers.
 * Operates on store, so not persisted identities won't be available
 * 
 * Additionally handles operations on the currently logged user, accessed via securityAccessor.
 * 
 * Exposed methods should include realm, to identify the invocation: 
 * users are a representation of a subject as visible from a realm.
 */

@Service
public class UserManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserService userService;

    @Autowired
    private UserEntityService userEntityService;

//    @Autowired
//    private RoleService roleService;

//    @Autowired
//    private AuthorityManager authorityManager;

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private SearchableApprovalStore approvalStore;

    @Autowired
    private ClientEntityService clientService;

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
        return userService.getUser(curUserDetails(), r.getSlug());
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
//    // source realm view, complete
//    public User getUser(String subjectId) throws NoSuchUserException {
//        return userService.getUser(subjectId);
//    }

    // per-realm view, partial and translated
    @Transactional(readOnly = true)
    public User getUser(String realm, String subjectId) throws NoSuchUserException, NoSuchRealmException {
        logger.debug("get user " + String.valueOf(subjectId) + " for realm " + realm);

        Realm r = realmService.getRealm(realm);
        // TODO evaluate if every user is globally accessible via translation or if we
        // require a pre-registration
        return userService.getUser(subjectId, r.getSlug());
    }

    // per realm view, lists both owned and proxied
    @Transactional(readOnly = true)
    public List<User> listUsers(String realm) throws NoSuchRealmException {
        logger.debug("list users for realm " + realm);

        Realm r = realmService.getRealm(realm);
        return userService.listUsers(r.getSlug());
    }

    /**
     * @param realm
     * @return
     * @throws NoSuchRealmException
     */
    @Transactional(readOnly = true)
    public long countUsers(String realm) throws NoSuchRealmException {
        logger.debug("count users for realm " + realm);

        Realm r = realmService.getRealm(realm);
        return userService.countUsers(r.getSlug());
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String realm, String keywords, Pageable pageRequest) throws NoSuchRealmException {
        logger.debug("search users for realm " + realm + " with keywords " + String.valueOf(keywords));
        Realm r = realmService.getRealm(realm);
        return userService.searchUsers(r.getSlug(), keywords, pageRequest);
    }

    /**
     * @param slug
     * @param subjectId
     * @param roles
     * @throws NoSuchUserException
     * @throws NoSuchRealmException
     */
    @Transactional(readOnly = false)
    public void updateRealmAuthorities(String realm, String subjectId, List<String> roles)
            throws NoSuchUserException, NoSuchRealmException {
        logger.debug("update authorities for user " + String.valueOf(subjectId) + " in realm " + realm);
        if (logger.isTraceEnabled()) {
            logger.trace("authorities: " + String.valueOf(roles));
        }

        Realm r = realmService.getRealm(realm);
        userService.updateRealmAuthorities(r.getSlug(), subjectId, roles);
    }

    @Transactional(readOnly = false)
    public void removeUser(String realm, String subjectId) throws NoSuchUserException, NoSuchRealmException {
        logger.debug("remove user " + String.valueOf(subjectId) + " from realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get user source realm
        String source = userService.getUserRealm(subjectId);
        if (source.equals(r.getSlug())) {
            // full delete
            deleteUser(subjectId);
        } else {
            // let userService handle account, registrations etc
            userService.removeUser(subjectId, r.getSlug());
        }
    }

    private void deleteUser(String subjectId) throws NoSuchUserException {
        logger.debug("delete user " + String.valueOf(subjectId));

        User user = userService.findUser(subjectId);
        if (user == null) {
            throw new NoSuchUserException();
        }

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

    public void inviteUser(String realm, String username, String subjectId, List<String> roles)
            throws NoSuchRealmException, NoSuchProviderException, RegistrationException, NoSuchUserException {

        logger.debug("invite user to realm" + realm);

        Realm r = realmService.getRealm(realm);

        if (StringUtils.hasText(username)) {
            Collection<IdentityProvider> providers = providerManager.getIdentityProviders(realm);

            // Assume internal provider exists and is unique
            Optional<IdentityProvider> internalProvider = providers.stream()
                    .filter(p -> p.getAuthority().equals(SystemKeys.AUTHORITY_INTERNAL)).findFirst();
            if (!internalProvider.isPresent()) {
                throw new NoSuchProviderException("No internal provider available");
            }

            IdentityService identityService = providerManager.getIdentityService(internalProvider.get().getProvider());

            InternalUserAccount account = new InternalUserAccount();
            account.setUsername(username);
            account.setEmail(username);
            account.setRealm(realm);

            UserIdentity identity = identityService.registerIdentity(null, account, Collections.emptyList());
            updateRealmAuthorities(realm, ((InternalUserAccount) identity.getAccount()).getSubject(), roles);
        }

        if (StringUtils.hasText(subjectId)) {
            User user = userService.findUser(subjectId);
            if (user == null) {
                throw new NoSuchUserException("No user with specified subjectId exist");
            }
            updateRealmAuthorities(realm, subjectId, roles);
        }
    }

    @Transactional(readOnly = false)
    public User blockUser(String realm, String subjectId) throws NoSuchUserException, NoSuchRealmException {
        logger.debug("block user " + String.valueOf(subjectId) + " from realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get user source realm
        String source = userService.getUserRealm(subjectId);
        if (source.equals(r.getSlug())) {
            // lock account
            userEntityService.blockUser(subjectId);
        }

        return userService.getUser(subjectId, realm);
    }

    @Transactional(readOnly = false)
    public User unblockUser(String realm, String subjectId) throws NoSuchUserException, NoSuchRealmException {
        logger.debug("block user " + String.valueOf(subjectId) + " from realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get user source realm
        String source = userService.getUserRealm(subjectId);
        if (source.equals(r.getSlug())) {
            // lock account
            userEntityService.unblockUser(subjectId);
        }

        return userService.getUser(subjectId, realm);
    }

    @Transactional(readOnly = false)
    public User lockUser(String realm, String subjectId) throws NoSuchUserException, NoSuchRealmException {
        logger.debug("lock user " + String.valueOf(subjectId) + " from realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get user source realm
        String source = userService.getUserRealm(subjectId);
        if (source.equals(r.getSlug())) {
            // lock account
            userEntityService.lockUser(subjectId);
        }

        return userService.getUser(subjectId, realm);
    }

    @Transactional(readOnly = false)
    public User unlockUser(String realm, String subjectId) throws NoSuchUserException, NoSuchRealmException {
        logger.debug("lock user " + String.valueOf(subjectId) + " from realm " + realm);

        Realm r = realmService.getRealm(realm);

        // get user source realm
        String source = userService.getUserRealm(subjectId);
        if (source.equals(r.getSlug())) {
            // lock account
            userEntityService.unlockUser(subjectId);
        }

        return userService.getUser(subjectId, realm);
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
     * Attributes (inside + outside accounts)
     * 
     * TODO
     */

    public Collection<UserAttributes> getMyAttributes() {
        UserDetails details = curUserDetails();
        Set<UserAttributes> attributes = new HashSet<>();

        // add all attributes from context
        attributes.addAll(details.getAttributeSets());

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
    public Collection<ConnectedAppProfile> getMyConnectedApps() {
        UserDetails details = curUserDetails();
        return getConnectedApps(details.getSubjectId());
    }

    public void deleteMyConnectedApp(String clientId) {
        UserDetails details = curUserDetails();
        deleteConnectedApp(details.getSubjectId(), clientId);
    }

    /*
     * Connected apps: subjects
     */
    @Transactional(readOnly = false)
    public Collection<ConnectedAppProfile> getConnectedApps(String realm, String subjectId) {

        // we return only clients which belong to the given realm
        List<ConnectedAppProfile> apps = getConnectedApps(subjectId).stream()
                .filter(a -> a.getRealm().equals(realm))
                .collect(Collectors.toList());

        return apps;
    }

    @Transactional(readOnly = false)
    public void deleteConnectedApp(String realm, String subjectId, String clientId)
            throws NoSuchUserException, NoSuchClientException {

        // get registrations, we need to match realm to client
        ConnectedAppProfile app = getConnectedApp(subjectId, clientId);
        if (app != null && app.getRealm().equals(realm)) {
            // valid registration in approval store, remove
            deleteConnectedApp(subjectId, clientId);
        }
    }

    /*
     * Connected apps: service
     * 
     * TODO evaluate move to dedicated service
     */
    private ConnectedAppProfile getConnectedApp(String subjectId, String clientId)
            throws NoSuchUserException, NoSuchClientException {

        ClientEntity client = clientService.getClient(clientId);
        Collection<Approval> approvals = approvalStore.getApprovals(subjectId, clientId);
        if (approvals.isEmpty()) {
            return null;
        }

        List<Scope> scopes = new ArrayList<>();

        for (Approval appr : approvals) {
            try {

                Scope scope = scopeRegistry.getScope(appr.getScope());
                scopes.add(scope);

            } catch (NoSuchScopeException e) {
                // scope does not exists
                // we should remove the approval
                approvalStore.revokeApprovals(Collections.singleton(appr));
            }
        }

        ConnectedAppProfile app = new ConnectedAppProfile(clientId, client.getName(), client.getRealm(),
                scopes);

        return app;
    }

    private List<ConnectedAppProfile> getConnectedApps(String subjectId) {

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

        List<ConnectedAppProfile> apps = map.entrySet().stream()
                .map(e -> new ConnectedAppProfile(e.getKey().getClientId(), e.getKey().getName(), e.getKey().getRealm(),
                        e.getValue()))
                .collect(Collectors.toList());

        return apps;
    }

    private void deleteConnectedApp(String subjectId, String clientId) {

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

//	/**
//	 * @param subjectId
//	 * @return
//	 * @throws NoSuchUserException 
//	 * @throws NoSuchRealmException 
//	 */
//	public Collection<SpaceRole> getMyRoles() throws NoSuchUserException, NoSuchRealmException {
//        UserDetails user = authHelper.getUserDetails();
//        if (user == null) {
//        	throw new NoSuchUserException();
//        }
//        Collection<SpaceRole> roles = new HashSet<>(userService.getUserRoles(user.getRealm(), user.getSubjectId()));
//        if (user.isSystemAdmin()) {
//        	roles.add(new SpaceRole(null, null, Config.R_PROVIDER));
//        }
//        return roles;
//	}

//	/**
//	 * @param context
//	 * @param q
//	 * @param pageRequest
//	 * @return
//	 */
//	public Page<SpaceRoles> getContextRoles(String context, String space, String q, Pageable pageRequest) {
//		return userService.getContextRoles(context, space, q, pageRequest);
//	}
//
//	/**
//	 * @param subject
//	 * @param context
//	 * @param space
//	 * @param roles
//	 * @return
//	 */
//	public SpaceRoles saveContextRoles(String subject, String context, String space, List<String> roles) {
//		return userService.saveContextRoles(subject, context, space, roles);
//	}

}
