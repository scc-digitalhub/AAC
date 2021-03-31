package it.smartcommunitylab.aac.core.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.UserTranslatorService;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.roles.RoleService;

/*
 * User management
 * 
 * Uses providers and/or services exposed by authorities.
 * We don't support users managed by offline/unavailable providers
 * 
 * TODO evaluate how to handle unavailable providers
 * 
 */
@Service
public class UserService {

    @Autowired
    private AuthorityManager authorityManager;

    // base services for users
    @Autowired
    private UserEntityService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserTranslatorService translator;

    public String getUserRealm(String subjectId) throws NoSuchUserException {
        UserEntity u = userService.getUser(subjectId);
        return u.getRealm();
    }

    /*
     * Returns a model describing the given user as accessible for the given realm.
     * 
     * For same-realm scenarios the model will be complete, while on cross-realm
     * some fields should be removed or empty.
     */
    public User getUser(String subjectId, String realm) throws NoSuchUserException {
        // resolve subject
        UserEntity u = userService.getUser(subjectId);
        Set<UserIdentity> identities = new HashSet<>();
        Set<UserAttributes> attributes = Collections.emptySet();

        if (u.getRealm().equals(realm)) {
            // same realm, fetch all
            // TODO we need an order criteria
            for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
                List<IdentityProvider> idps = ia.getIdentityProviders(realm);
                for (IdentityProvider idp : idps) {
                    identities.addAll(idp.listIdentities(subjectId));
                }
            }

            // TODO
            attributes = Collections.emptySet();
        } else {
            // fetch accessible
            // TODO decide policy + implement
        }

        User user = new User(subjectId, u.getRealm());
        for (UserIdentity identity : identities) {
            user.addIdentity(identity);
        }
        for (UserAttributes attr : attributes) {
            user.addAttributes(attr);
        }

        // let translator filter content according to policy
        user = translator.translate(user, realm);

        // fetch authorities under the given realm
        Set<RealmGrantedAuthority> authorities = new HashSet<>();
        List<UserRoleEntity> userRoles = userService.getRoles(subjectId, realm);

        for (UserRoleEntity ur : userRoles) {
            authorities.add(new RealmGrantedAuthority(ur.getRealm(), ur.getRole()));
        }

        // fetch space roles, always available
        Set<SpaceRole> roles = roleService.getRoles(subjectId);

        user.setAuthorities(authorities);
        user.setRoles(roles);

        return user;

    }

    /*
     * Lists users under the given realm
     * 
     * TODO find a method to include users owned by different realms but
     * "accessible" from this realm.
     */

    public List<User> listUsers(String realm) {
        // owned by realm
        List<UserEntity> users = userService.getUsers(realm);
        List<User> realmUsers = users.stream()
                .map(u -> {
                    try {
                        return getUser(u.getUuid(), realm);
                    } catch (NoSuchUserException e) {
                        return null;
                    }
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());

        // accessible from this realm
        // TODO

        // TODO translate resulting users
        return realmUsers;

    }

    /*
     * Remove a user from the given realm
     *
     * if realm matches source realm user will be deleted, otherwise only the proxy
     * will be dropped TODO cross realm
     */

    public void removeUser(String subjectId, String realm) throws NoSuchUserException {
        UserEntity user = userService.getUser(subjectId);

        if (user.getRealm().equals(realm)) {
            // same realm, delete

            // delete provider registrations

            // delete user
            userService.deleteUser(subjectId);

        } else {
            // fetch accessible
            // TODO decide policy + implement
        }

    }

    // TODO user registration with authority via given provider
    // TODO user removal with authority via given provider

}
