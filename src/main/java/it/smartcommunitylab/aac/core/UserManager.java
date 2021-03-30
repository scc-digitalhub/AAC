package it.smartcommunitylab.aac.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntity;
import it.smartcommunitylab.aac.core.provider.UserService;
import it.smartcommunitylab.aac.core.service.AttributeEntityService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.InternalUserManager;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.openid.OIDCUserManager;
import it.smartcommunitylab.aac.roles.RoleService;

/*
 * Manager for users
 * 
 * should handle all the operations on accounts, by relying on authority managers.
 * Operates on store, so not persisted identities won't be available
 * 
 * Additionally handles operations on the currently logged user, accessed via securiyAccessor.
 */

@Service
public class UserManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // base services for users
    @Autowired
    private UserEntityService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    private AuthenticationHelper authHelper;

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

    public User curUser() {
        UserDetails details = curUserDetails();
        String subjectId = details.getSubjectId();
        String realm = details.getRealm();

        Set<SpaceRole> roles = roleService.getRoles(subjectId);
        User user = buildUser(subjectId, realm,
                details.getIdentities(), details.getAttributeSets(),
                roles);

        return user;
    }

    /*
     * Manage users
     */

    /*
     * UserDetails describes user in terms of identities as authorities and
     * authentication
     */
    public UserDetails getUserDetails(String subjectId, String realm) throws NoSuchUserException {
        // resolve subject
        UserEntity user = userService.getUser(subjectId);
        if (!user.getRealm().equals(realm)) {
            throw new NoSuchUserException("realm mismatch");
        }

        // fetch authorities, only for this realm
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(Config.R_USER));
        List<UserRoleEntity> userRoles = userService.getRoles(subjectId, realm);

        for (UserRoleEntity ur : userRoles) {
            authorities.add(new RealmGrantedAuthority(ur.getRealm(), ur.getRole()));
        }

        // fetch identities
        Set<UserIdentity> identities = new HashSet<>();
        // TODO we need an order criteria
        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
            UserService service = ia.getUserService();
            identities.addAll(service.listUsers(subjectId));
        }

        // fetch attributes outside idps
        // TODO
        Set<UserAttributes> attributes = Collections.emptySet();

        // build userDetails
        return buildUserDetails(subjectId, realm,
                identities, attributes, authorities);

    }

    /*
     * User describes user in terms of identities as attributes
     */
    public User getUser(String subjectId, String realm) throws NoSuchUserException {
        // resolve subject
        UserEntity user = userService.getUser(subjectId);
        if (!user.getRealm().equals(realm)) {
            throw new NoSuchUserException("realm mismatch");
        }

        // fetch identities
        Set<UserIdentity> identities = new HashSet<>();
        // TODO we need an order criteria
        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
            UserService service = ia.getUserService();
            identities.addAll(service.listUsers(subjectId));
        }

        // fetch attributes outside idps
        // TODO
        Set<UserAttributes> attributes = Collections.emptySet();

        Set<SpaceRole> roles = roleService.getRoles(subjectId);

        return buildUser(subjectId, realm,
                identities, attributes, roles);

    }

    public List<User> listUsers(String realm) {
        List<UserEntity> users = userService.getUsers(realm);
        return users.stream()
                .map(u -> {
                    try {
                        return getUser(u.getUuid(), realm);
                    } catch (NoSuchUserException e) {
                        return null;
                    }
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());

    }

    public void removeUser(String subjectId, String realm) {
        // TODO
    }

    /*
     * registration
     */
    public User registerUser() {

    }

    public UserIdentity registerUserIdentity() {

    }

    /*
     * credentials
     */
    public UserCredentials getUserCredentials(String subjectId, String realm, String userId) {

    }

    public UserCredentials resetUserCredentials();

    public String getResetUserCredentialsLink();

    public UserCredentials setUserCredentials();

    /*
     * 2FA/MFA
     * 
     * TODO add dedicated TOTP/HTOP credentials provider
     */

    /*
     * helpers
     */

    private User buildUser(String subjectId, String realm, Collection<UserIdentity> identities,
            Collection<UserAttributes> attributes, Set<SpaceRole> roles) {
        User user = new User(subjectId, realm);
        for (UserIdentity identity : identities) {
            user.addIdentity(identity);
        }
        for (UserAttributes attr : attributes) {
            user.addAttributes(attr);
        }

        user.setRoles(null);

        return user;
    }

    private UserDetails buildUserDetails(String subjectId, String realm, Collection<UserIdentity> identities,
            Collection<UserAttributes> attributes, Collection<? extends GrantedAuthority> authorities) {
        // TODO add ordering for identities
        return new UserDetails(subjectId, realm, identities, attributes, authorities);

    }

}
