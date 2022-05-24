package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
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
                .map(e -> new ConnectedAppProfile(subjectId, e.getKey().getClientId(), e.getKey().getRealm(),
                        e.getKey().getName(),
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

    /*
     * Providers
     */

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
     * Space roles
     */
//  public Collection<SpaceRole> getMyRoles() throws NoSuchUserException, NoSuchRealmException {
//  UserDetails user = authHelper.getUserDetails();
//  if (user == null) {
//    throw new NoSuchUserException();
//  }
//  Collection<SpaceRole> roles = new HashSet<>(userService.getUserRoles(user.getRealm(), user.getSubjectId()));
//  if (user.isSystemAdmin()) {
//    roles.add(new SpaceRole(null, null, Config.R_PROVIDER));
//  }
//  return roles;
//}
}
