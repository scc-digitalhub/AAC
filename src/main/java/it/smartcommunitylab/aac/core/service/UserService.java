package it.smartcommunitylab.aac.core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.authorities.AttributeAuthority;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntity;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeService;
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
 * TODO evaluate cache on translators/fetch
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
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserTranslatorService translator;

    /*
     * User translation
     */
    public User getUser(UserDetails userDetails) {
        String subjectId = userDetails.getSubjectId();
        String realm = userDetails.getRealm();

        User u = new User(userDetails);

        try {
            UserEntity ue = userService.getUser(subjectId);
            // refresh attributes
            u.setExpirationDate(ue.getExpirationDate());
            u.setCreateDate(ue.getCreateDate());
            u.setModifiedDate(ue.getModifiedDate());
            u.setLoginDate(ue.getLoginDate());
            u.setLoginIp(ue.getLoginIp());
            u.setLoginProvider(ue.getLoginProvider());

            // refresh authorities
            u.setAuthorities(fetchUserRealmAuthorities(subjectId, realm));

            // refresh user attributes
            u.setAttributes(fetchUserAttributes(subjectId, realm));

            // refresh space roles
            u.setRoles(fetchUserSpaceRoles(subjectId, realm));

        } catch (NoSuchUserException e) {
            // something wrong with refresh, ignore
        }

        return u;
    }

    public User getUser(UserDetails userDetails, String realm) {
        String subjectId = userDetails.getSubjectId();

        if (realm == null || userDetails.getRealm().equals(realm)) {
            // no translation needed, just refresh
            return getUser(userDetails);
        }

        // translate details via translator
        // this will support per-realm translators and fine-grained policies
        User u = translator.translate(userDetails, realm);

        try {
            UserEntity ue = userService.getUser(subjectId);
            // refresh attributes
            u.setExpirationDate(ue.getExpirationDate());
            u.setCreateDate(ue.getCreateDate());
            u.setModifiedDate(ue.getModifiedDate());
            u.setLoginDate(ue.getLoginDate());
            u.setLoginIp(ue.getLoginIp());
            u.setLoginProvider(ue.getLoginProvider());

            // refresh authorities
            u.setAuthorities(fetchUserRealmAuthorities(subjectId, realm));

            // refresh user attributes
            u.setAttributes(fetchUserAttributes(subjectId, realm));

            // refresh space roles
            u.setRoles(fetchUserSpaceRoles(subjectId, realm));
        } catch (NoSuchUserException e) {
            // something wrong with refresh, ignore
        }

        return u;
    }

    public User getUser(User user, String realm) {
        String subjectId = user.getSubjectId();

        if (realm == null || user.getRealm().equals(realm)) {
            // no translation needed
            // TODO evaluate refresh
            return user;
        }

        // translate details via translator
        // this will support per-realm translators and fine-grained policies
        User u = translator.translate(user, realm);

        try {
            UserEntity ue = userService.getUser(subjectId);
            // refresh attributes
            u.setExpirationDate(ue.getExpirationDate());
            u.setCreateDate(ue.getCreateDate());
            u.setModifiedDate(ue.getModifiedDate());
            u.setLoginDate(ue.getLoginDate());
            u.setLoginIp(ue.getLoginIp());
            u.setLoginProvider(ue.getLoginProvider());

            // refresh authorities
            u.setAuthorities(fetchUserRealmAuthorities(subjectId, realm));

            // refresh user attributes
            u.setAttributes(fetchUserAttributes(subjectId, realm));

            // refresh space roles
            u.setRoles(fetchUserSpaceRoles(subjectId, realm));
        } catch (NoSuchUserException e) {
            // something wrong with refresh, ignore
        }

        return u;
    }

    /*
     * User management
     */

    public String getUserRealm(String subjectId) throws NoSuchUserException {
        UserEntity u = userService.getUser(subjectId);
        return u.getRealm();
    }

    public User findUser(String subjectId) {
        UserEntity u = userService.findUser(subjectId);
        if (u == null) {
            return null;
        }

        String realm = u.getRealm();

        User user = new User(subjectId, u.getRealm());

        // add authorities
        try {
            user.setAuthorities(fetchUserRealmAuthorities(subjectId, realm));
        } catch (NoSuchUserException e) {
            // ignore
        }

        // no identities, attributes etc
        return user;
    }

    public User getUser(String subjectId) throws NoSuchUserException {
        // resolve subject
        UserEntity ue = userService.getUser(subjectId);
        String realm = ue.getRealm();

        User u = new User(subjectId, ue.getRealm());
        u.setUsername(ue.getUsername());

        // fetch attributes
        u.setExpirationDate(ue.getExpirationDate());
        u.setCreateDate(ue.getCreateDate());
        u.setModifiedDate(ue.getModifiedDate());
        u.setLoginDate(ue.getLoginDate());
        u.setLoginIp(ue.getLoginIp());
        u.setLoginProvider(ue.getLoginProvider());

        Set<UserIdentity> identities = new HashSet<>();

        // same realm, fetch all idps
        // TODO we need an order criteria
        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
            List<IdentityProvider> idps = ia.getIdentityProviders(realm);
            for (IdentityProvider idp : idps) {
                identities.addAll(idp.listIdentities(subjectId));
            }
        }

        for (UserIdentity identity : identities) {
            u.addIdentity(identity);
        }

        // add authorities
        u.setAuthorities(fetchUserRealmAuthorities(subjectId, realm));

        // add user attributes
        u.addAttributes(fetchUserAttributes(subjectId, realm));

        // add space roles
        u.setRoles(fetchUserSpaceRoles(subjectId, realm));

        return u;

    }

    /*
     * Returns a model describing the given user as accessible for the given realm.
     * 
     * For same-realm scenarios the model will be complete, while on cross-realm
     * some fields should be removed or empty.
     */
    public User getUser(String subjectId, String realm) throws NoSuchUserException {
        // resolve subject
        UserEntity ue = userService.getUser(subjectId);
        String source = ue.getRealm();

        User u = new User(subjectId, ue.getRealm());
        u.setUsername(ue.getUsername());
        u.setEmail(ue.getEmailAddress());

        // fetch attributes
        u.setExpirationDate(ue.getExpirationDate());
        u.setCreateDate(ue.getCreateDate());
        u.setModifiedDate(ue.getModifiedDate());
        u.setLoginDate(ue.getLoginDate());
        u.setLoginIp(ue.getLoginIp());
        u.setLoginProvider(ue.getLoginProvider());

        Set<UserIdentity> identities = new HashSet<>();

        // fetch all identities from source realm
        // TODO we need an order criteria
        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
            List<IdentityProvider> idps = ia.getIdentityProviders(source);
            for (IdentityProvider idp : idps) {
                identities.addAll(idp.listIdentities(subjectId));
            }
        }
        if (!source.equals(realm)) {
            // also fetch identities from destination realm
            // TODO we need an order criteria
            for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
                List<IdentityProvider> idps = ia.getIdentityProviders(realm);
                for (IdentityProvider idp : idps) {
                    identities.addAll(idp.listIdentities(subjectId));
                }
            }
        }

        for (UserIdentity identity : identities) {
            u.addIdentity(identity);
        }

        // TODO evaluate loading source realm attributes to feed translator?

        if (!source.equals(realm)) {
            // let translator filter content according to policy
            u = translator.translate(u, realm);
        }

        // add authorities
        u.setAuthorities(fetchUserRealmAuthorities(subjectId, realm));

        // add user attributes
        u.setAttributes(fetchUserAttributes(subjectId, realm));

        // add space roles
        u.setRoles(fetchUserSpaceRoles(subjectId, realm));

        return u;

    }

    /*
     * Lists users under the given realm
     * 
     * TODO find a method to include users owned by different realms but
     * "accessible" from this realm.
     */

    public List<User> listUsers(String realm) {
        // owned by realm
        List<UserEntity> users = userService.listUsers(realm);
        return convertUsers(realm, users);

    }

    public Long countUsers(String realm) {
        // TODO Auto-generated method stub
        return userService.countUsers(realm);
    }

    public Page<User> searchUsers(String realm, String q, Pageable pageRequest) {
        Page<UserEntity> page = userService.searchUsers(realm, q, pageRequest);
        return PageableExecutionUtils.getPage(
                convertUsers(realm, page.getContent()),
                pageRequest,
                () -> page.getTotalElements());
    }

    protected List<User> convertUsers(String realm, List<UserEntity> users) {
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

    /**
     * Update realm roles for the specified user
     * 
     * @param slug
     * @param subjectId
     * @param roles
     * @throws NoSuchUserException
     */
    public void updateRealmAuthorities(String slug, String subjectId, List<String> roles) throws NoSuchUserException {
        //check role format
        roles.stream().forEach(r -> {
            if (!StringUtils.hasText(r) || !r.matches(SystemKeys.SLUG_PATTERN)) {
                throw new IllegalArgumentException("invalid role format, valid chars "+SystemKeys.SLUG_PATTERN);
            }
        });
        
        //update
        userService.updateRoles(subjectId, slug, roles);
    }

    /**
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
            // CURRENTLY ONLY DROP REALM ROLES
            updateRealmAuthorities(realm, subjectId, Collections.emptyList());
        }

    }

    public void deleteUser(String subjectId) throws NoSuchUserException {
        UserEntity user = userService.getUser(subjectId);
        String realm = user.getRealm();

        // delete identities via providers
        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
            List<IdentityProvider> idps = ia.getIdentityProviders(realm);
            for (IdentityProvider idp : idps) {
                // remove all identities
                idp.deleteIdentities(subjectId);
            }
        }

        // attributes
        for (AttributeAuthority aa : authorityManager.listAttributeAuthorities()) {
            List<AttributeProvider> aps = aa.getAttributeProviders(realm);
            for (AttributeProvider ap : aps) {
                // remove all attributes
                ap.deleteAttributes(subjectId);

            }
        }
        // roles
        roleService.deleteRoles(subjectId);

        // delete user
        userService.deleteUser(subjectId);

    }

    // TODO user registration with authority via given provider
    // TODO user removal with authority via given provider

    /*
     * User Attributes
     */
    public Collection<UserAttributes> getUserAttributes(String subjectId, String realm) throws NoSuchUserException {
        UserEntity u = userService.getUser(subjectId);

        return fetchUserAttributes(u.getUuid(), realm);
    }

    public Collection<UserAttributes> getUserAttributes(String subjectId, String realm, String provider)
            throws NoSuchUserException, NoSuchProviderException {
        UserEntity u = userService.getUser(subjectId);
        AttributeProvider ap = authorityManager.getAttributeProvider(provider);
        return ap.getAttributes(u.getUuid());
    }

    public UserAttributes getUserAttributes(String subjectId, String realm, String provider, String setId)
            throws NoSuchUserException, NoSuchProviderException {
        UserEntity u = userService.getUser(subjectId);
        ConfigurableAttributeProvider cp = attributeProviderService.getProvider(provider);
        if (!cp.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }
        if (!cp.getAttributeSets().contains(setId)) {
            throw new IllegalArgumentException("set not enabled for this provider");
        }

        AttributeProvider ap = authorityManager.fetchAttributeProvider(cp.getAuthority(), provider);
        return ap.getAttributes(u.getUuid()).stream().filter(a -> a.getIdentifier().equals(setId)).findFirst()
                .orElse(null);
    }

    public UserAttributes setUserAttributes(String subjectId, String realm, String provider,
            AttributeSet attributeSet) throws NoSuchUserException, NoSuchProviderException {
        UserEntity u = userService.getUser(subjectId);
        ConfigurableAttributeProvider cp = attributeProviderService.getProvider(provider);
        if (!cp.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }
        String setId = attributeSet.getIdentifier();
        if (!cp.getAttributeSets().contains(setId)) {
            throw new IllegalArgumentException("set not enabled for this provider");
        }

        AttributeService as = authorityManager.fetchAttributeService(cp.getAuthority(), provider);
        return as.putAttributes(subjectId, Collections.singleton(attributeSet)).stream().findFirst().orElse(null);
    }

    /*
     * Helpers
     */

    private Collection<UserAttributes> fetchUserAttributes(String subjectId, String realm) throws NoSuchUserException {
        List<UserAttributes> attributes = new ArrayList<>();
        // fetch from providers
        for (AttributeAuthority aa : authorityManager.listAttributeAuthorities()) {
            List<AttributeProvider> aps = aa.getAttributeProviders(realm);
            for (AttributeProvider ap : aps) {
                attributes.addAll(ap.getAttributes(subjectId));
            }
        }

        return attributes;
    }

    private Collection<GrantedAuthority> fetchUserRealmAuthorities(String subjectId, String realm)
            throws NoSuchUserException {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(Config.R_USER));

        // fetch all authoritites for realm
        List<UserRoleEntity> realmRoles = userService.getRoles(subjectId, realm);
        List<RealmGrantedAuthority> realmAuthorities = realmRoles.stream()
                .map(ur -> new RealmGrantedAuthority(ur.getRealm(), ur.getRole()))
                .collect(Collectors.toList());
        authorities.addAll(realmAuthorities);

        // also add global authorities
        List<UserRoleEntity> globalRoles = userService.getRoles(subjectId, SystemKeys.REALM_GLOBAL);
        List<SimpleGrantedAuthority> globalAuthorities = globalRoles.stream()
                .map(ur -> new SimpleGrantedAuthority(ur.getRole()))
                .collect(Collectors.toList());
        authorities.addAll(globalAuthorities);

        return authorities;

    }

    private Collection<SpaceRole> fetchUserSpaceRoles(String subjectId, String realm) throws NoSuchUserException {
        // we don't filter space roles per realm, so read all
        return roleService.getRoles(subjectId);
    }

}
