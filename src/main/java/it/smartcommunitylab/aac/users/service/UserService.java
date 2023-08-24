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

package it.smartcommunitylab.aac.users.service;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.service.AccountServiceAuthorityService;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.attributes.provider.AttributeProvider;
import it.smartcommunitylab.aac.attributes.service.AttributeProviderAuthorityService;
import it.smartcommunitylab.aac.attributes.service.AttributeProviderService;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.credentials.service.CredentialsServiceAuthorityService;
import it.smartcommunitylab.aac.groups.service.GroupService;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.identity.provider.IdentityProvider;
import it.smartcommunitylab.aac.identity.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.internal.InternalAttributeAuthority;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeService;
import it.smartcommunitylab.aac.model.Group;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;
import it.smartcommunitylab.aac.roles.service.SubjectRoleService;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // base services for users
    @Autowired
    private UserEntityService userService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SubjectRoleService roleService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private IdentityProviderAuthorityService identityProviderAuthorityService;

    @Autowired
    private AttributeProviderAuthorityService attributeProviderAuthorityService;

    @Autowired
    private AccountServiceAuthorityService accountServiceAuthorityService;

    @Autowired
    private CredentialsServiceAuthorityService credentialsServiceAuthorityService;

    @Autowired
    private InternalAttributeAuthority internalAttributeAuthority;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private SpaceRoleService spaceRoleService;

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
            boolean tosAccepted = ue.getTosAccepted() != null ? ue.getTosAccepted().booleanValue() : false;
            u.setTosAccepted(tosAccepted);

            // refresh authorities
            u.setAuthorities(fetchUserAuthorities(subjectId, realm));

            // refresh user attributes
            u.setAttributes(fetchUserAttributes(subjectId, realm));

            // refresh groups
            u.setGroups(fetchUserGroups(subjectId, realm));
            Set<String> groupIds = u.getGroups().stream().map(g -> g.getGroupId()).collect(Collectors.toSet());

            // refresh realm roles
            u.setRealmRoles(fetchUserRealmRoles(subjectId, realm, groupIds));

            // refresh space roles
            u.setSpaceRoles(fetchUserSpaceRoles(subjectId, realm));
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
            boolean tosAccepted = ue.getTosAccepted() != null ? ue.getTosAccepted().booleanValue() : false;
            u.setTosAccepted(tosAccepted);

            // refresh authorities
            u.setAuthorities(fetchUserAuthorities(subjectId, realm));

            // refresh user attributes
            u.setAttributes(fetchUserAttributes(subjectId, realm));

            // refresh groups
            u.setGroups(fetchUserGroups(subjectId, realm));
            Set<String> groupIds = u.getGroups().stream().map(g -> g.getGroupId()).collect(Collectors.toSet());

            // refresh realm roles
            u.setRealmRoles(fetchUserRealmRoles(subjectId, realm, groupIds));

            // refresh space roles
            u.setSpaceRoles(fetchUserSpaceRoles(subjectId, realm));
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
            u.setAuthorities(fetchUserAuthorities(subjectId, realm));

            // refresh user attributes
            u.setAttributes(fetchUserAttributes(subjectId, realm));

            // refresh groups
            u.setGroups(fetchUserGroups(subjectId, realm));
            Set<String> groupIds = u.getGroups().stream().map(g -> g.getGroupId()).collect(Collectors.toSet());

            // refresh realm roles
            u.setRealmRoles(fetchUserRealmRoles(subjectId, realm, groupIds));

            // refresh space roles
            u.setSpaceRoles(fetchUserSpaceRoles(subjectId, realm));
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
            user.setAuthorities(fetchUserAuthorities(subjectId, realm));
            user.setTosAccepted(u.getTosAccepted());
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
        u.setEmail(ue.getEmailAddress());
        boolean emailVerified = ue.getEmailVerified() != null ? ue.getEmailVerified().booleanValue() : false;
        u.setEmailVerified(emailVerified);

        if (ue.getTosAccepted() != null) {
            u.setTosAccepted(ue.isTosAccepted());
        } else {
            u.setTosAccepted(null);
        }

        // status
        SubjectStatus status = SubjectStatus.parse(ue.getStatus());
        u.setStatus(status);

        // fetch attributes
        u.setExpirationDate(ue.getExpirationDate());
        u.setCreateDate(ue.getCreateDate());
        u.setModifiedDate(ue.getModifiedDate());
        u.setLoginDate(ue.getLoginDate());
        u.setLoginIp(ue.getLoginIp());
        u.setLoginProvider(ue.getLoginProvider());

        // same realm, fetch all idps
        u.setIdentities(fetchUserIdentities(subjectId, realm));

        // add authorities
        u.setAuthorities(fetchUserAuthorities(subjectId, realm));

        // add user attributes
        u.addAttributes(fetchUserAttributes(subjectId, realm));

        // add groups
        u.setGroups(fetchUserGroups(subjectId, realm));
        Set<String> groupIds = u.getGroups().stream().map(g -> g.getGroupId()).collect(Collectors.toSet());

        // add realm roles
        u.setRealmRoles(fetchUserRealmRoles(subjectId, realm, groupIds));

        // add space roles
        u.setSpaceRoles(fetchUserSpaceRoles(subjectId, realm));

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
        boolean emailVerified = ue.getEmailVerified() != null ? ue.getEmailVerified().booleanValue() : false;
        u.setEmailVerified(emailVerified);

        if (ue.getTosAccepted() != null) {
            u.setTosAccepted(ue.isTosAccepted());
        } else {
            u.setTosAccepted(null);
        }

        // status
        SubjectStatus status = SubjectStatus.parse(ue.getStatus());
        u.setStatus(status);

        // fetch attributes
        u.setExpirationDate(ue.getExpirationDate());
        u.setCreateDate(ue.getCreateDate());
        u.setModifiedDate(ue.getModifiedDate());
        u.setLoginDate(ue.getLoginDate());
        u.setLoginIp(ue.getLoginIp());
        u.setLoginProvider(ue.getLoginProvider());

        u.setIdentities(fetchUserIdentities(subjectId, realm));

        // TODO evaluate loading source realm attributes to feed translator?

        if (!source.equals(realm)) {
            // let translator filter content according to policy
            u = translator.translate(u, realm);
        }

        // add authorities
        u.setAuthorities(fetchUserAuthorities(subjectId, realm));

        // add user attributes
        u.setAttributes(fetchUserAttributes(subjectId, realm));

        // add groups
        u.setGroups(fetchUserGroups(subjectId, realm));
        Set<String> groupIds = u.getGroups().stream().map(g -> g.getGroupId()).collect(Collectors.toSet());

        // add realm roles
        u.setRealmRoles(fetchUserRealmRoles(subjectId, realm, groupIds));

        // add space roles
        u.setSpaceRoles(fetchUserSpaceRoles(subjectId, realm));

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
            () -> page.getTotalElements()
        );
    }

    public Page<User> searchUsersWithSpec(String realm, Specification<UserEntity> spec, Pageable pageRequest) {
        Page<UserEntity> page = userService.searchUsersWithSpec(spec, pageRequest);
        return PageableExecutionUtils.getPage(
            convertUsers(realm, page.getContent()),
            pageRequest,
            () -> page.getTotalElements()
        );
    }

    protected List<User> convertUsers(String realm, List<UserEntity> users) {
        List<User> realmUsers = users
            .stream()
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

    public List<User> findUsersByUsername(String realm, String username) {
        return convertUsers(realm, userService.findUsersByUsername(realm, username));
    }

    public List<User> findUsersByEmailAddress(String realm, String emailAddress) {
        return convertUsers(realm, userService.findUsersByEmailAddress(realm, emailAddress));
    }

    public List<User> listUsersByAuthority(String realm, String role) {
        // with authority in realm
        List<Subject> subjects = subjectService.listSubjectsByAuthorities(realm, role);

        List<UserEntity> users = subjects
            .stream()
            .filter(s -> SystemKeys.RESOURCE_USER.equals(s.getType()))
            .map(s -> userService.findUser(s.getSubjectId()))
            .filter(s -> s != null)
            .collect(Collectors.toList());
        return convertUsers(realm, users);
    }

    //    /**
    //     * Update realm roles for the specified user
    //     *
    //     * @param slug
    //     * @param subjectId
    //     * @param roles
    //     * @throws NoSuchUserException
    //     */
    //    public Collection<RealmRole> updateRoles(String realm, String subjectId, Collection<String> roles)
    //            throws NoSuchUserException {
    //        // check role format
    //        roles.stream().forEach(r -> {
    //            if (!StringUtils.hasText(r) || !r.matches(SystemKeys.SLUG_PATTERN)) {
    //                throw new IllegalArgumentException("invalid role format, valid chars " + SystemKeys.SLUG_PATTERN);
    //            }
    //        });
    //
    //        // update
    //        List<UserRoleEntity> realmRoles = userService.updateRoles(subjectId, realm, roles);
    //        return realmRoles.stream()
    //                .map(ur -> new RealmRole(ur.getRealm(), ur.getRole()))
    //                .collect(Collectors.toList());
    //    }
    //
    //    public Collection<RealmRole> getRoles(String realm, String subjectId)
    //            throws NoSuchUserException {
    //        // fetch all authoritites for realm
    //        List<UserRoleEntity> realmRoles = userService.getRoles(subjectId, realm);
    //        return realmRoles.stream()
    //                .map(ur -> new RealmRole(ur.getRealm(), ur.getRole()))
    //                .collect(Collectors.toList());
    //    }

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
            //            updateRoles(realm, subjectId, Collections.emptyList());
        }
    }

    @Transactional(readOnly = false)
    public void deleteUser(String subjectId) throws NoSuchUserException {
        UserEntity user = userService.getUser(subjectId);
        String realm = user.getRealm();

        // delete identities via (active) providers
        identityProviderAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .forEach(p -> p.deleteIdentities(subjectId));

        // delete accounts via (active) services
        accountServiceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .forEach(s -> s.deleteAccounts(subjectId));

        // delete credentials via (active) services
        credentialsServiceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .forEach(s -> s.deleteCredentials(subjectId));

        // delete attributes via (active) providers
        attributeProviderAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .forEach(ap -> ap.deleteUserAttributes(subjectId));

        // roles
        spaceRoleService.deleteRoles(subjectId);

        // delete user
        userService.deleteUser(subjectId);
    }

    @Transactional(readOnly = false)
    public User blockUser(String userId) throws NoSuchUserException, NoSuchRealmException {
        userService.blockUser(userId);
        return getUser(userId);
    }

    @Transactional(readOnly = false)
    public User activateUser(String userId) throws NoSuchUserException, NoSuchRealmException {
        userService.activateUser(userId);
        return getUser(userId);
    }

    @Transactional(readOnly = false)
    public User inactivateUser(String userId) throws NoSuchUserException, NoSuchRealmException {
        userService.inactivateUser(userId);
        return getUser(userId);
    }

    // TODO user registration with authority via given provider
    // TODO user removal with authority via given provider

    /*
     * User authorities
     */

    public Collection<GrantedAuthority> getUserAuthorities(String subjectId, String realm) throws NoSuchUserException {
        UserEntity u = userService.getUser(subjectId);

        return fetchUserAuthorities(u.getUuid(), realm);
    }

    public Collection<GrantedAuthority> setUserAuthorities(String subjectId, String realm, Collection<String> roles)
        throws NoSuchUserException {
        UserEntity u = userService.getUser(subjectId);

        try {
            return subjectService.updateAuthorities(u.getUuid(), realm, roles);
        } catch (NoSuchSubjectException e) {
            throw new NoSuchUserException();
        }
    }

    /*
     * User Attributes
     */
    public Collection<UserAttributes> getUserAttributes(String subjectId, String realm) throws NoSuchUserException {
        UserEntity u = userService.getUser(subjectId);

        return fetchUserAttributes(u.getUuid(), realm);
    }

    public Collection<UserAttributes> getUserAttributes(String subjectId, String realm, String provider)
        throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        UserEntity u = userService.getUser(subjectId);

        // fetch config
        ConfigurableAttributeProvider cap = attributeProviderService.getProvider(provider);
        if (!cap.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        // fetch active
        AttributeProvider<?, ?, ?> ap = attributeProviderAuthorityService
            .getAuthority(cap.getAuthority())
            .getProvider(cap.getProvider());

        return ap.getUserAttributes(u.getUuid());
    }

    public UserAttributes getUserAttributes(String subjectId, String realm, String provider, String setId)
        throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        UserEntity u = userService.getUser(subjectId);

        // fetch config
        ConfigurableAttributeProvider cap = attributeProviderService.getProvider(provider);
        if (!cap.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        // if (!cap.getAttributeSets().contains(setId)) {
        //     throw new IllegalArgumentException("set not enabled for this provider");
        // }

        // fetch active
        AttributeProvider<?, ?, ?> ap = attributeProviderAuthorityService
            .getAuthority(cap.getAuthority())
            .getProvider(cap.getProvider());

        return ap
            .getUserAttributes(u.getUuid())
            .stream()
            .filter(a -> a.getIdentifier().equals(setId))
            .findFirst()
            .orElse(null);
    }

    public UserAttributes setUserAttributes(String subjectId, String realm, String provider, AttributeSet attributeSet)
        throws NoSuchUserException, NoSuchProviderException {
        UserEntity u = userService.getUser(subjectId);
        String setId = attributeSet.getIdentifier();

        // fetch config
        ConfigurableAttributeProvider cap = attributeProviderService.getProvider(provider);
        if (!cap.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        //DISABLED, enable removal even for not registered sets
        // if (!cap.getAttributeSets().contains(setId)) {
        //     throw new IllegalArgumentException("set not enabled for this provider");
        // }

        // supports only internal
        // TODO refactor
        InternalAttributeService as = internalAttributeAuthority.getProvider(provider);
        return as.putUserAttributes(subjectId, setId, attributeSet);
    }

    public void removeUserAttributes(String subjectId, String realm, String provider, String setId)
        throws NoSuchProviderException, NoSuchAuthorityException, NoSuchAttributeSetException {
        // fetch config
        ConfigurableAttributeProvider cap = attributeProviderService.getProvider(provider);
        if (!cap.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        //DISABLED, enable removal even for not registered sets
        // if (!cap.getAttributeSets().contains(setId)) {
        //     throw new IllegalArgumentException("set not enabled for this provider");
        // }

        // fetch active
        AttributeProvider<?, ?, ?> ap = attributeProviderAuthorityService
            .getAuthority(cap.getAuthority())
            .getProvider(cap.getProvider());

        // delete single set
        ap.deleteUserAttributes(subjectId, setId);
    }

    /*
     * User credentials
     */

    /*
     * Related data
     */

    public Collection<UserIdentity> fetchUserIdentities(String subjectId, String realm) throws NoSuchUserException {
        List<UserIdentity> identities = new ArrayList<>();
        // fetch all identities from source realm
        // TODO we need an order criteria
        Collection<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> providers = identityProviderAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .collect(Collectors.toList());
        for (IdentityProvider<? extends UserIdentity, ?, ?, ?, ?> idp : providers) {
            identities.addAll(idp.listIdentities(subjectId));
        }

        //        if (!source.equals(realm)) {
        // DISABLED, TODO evaluate cross realm
        //            // also fetch identities from destination realm
        //            // TODO we need an order criteria
        //            for (IdentityProviderAuthority<? extends UserIdentity> ia : authorityManager
        //                    .listIdentityAuthorities()) {
        //                List<? extends IdentityProvider<? extends UserIdentity>> idps = ia
        //                        .getProviders(realm);
        //                for (IdentityProvider<? extends UserIdentity> idp : idps) {
        //                    identities.addAll(idp.listIdentities(subjectId));
        //                }
        //            }
        //        }

        return identities;
    }

    public Collection<UserAttributes> fetchUserAttributes(String subjectId, String realm) throws NoSuchUserException {
        List<UserAttributes> attributes = new ArrayList<>();
        // fetch from providers
        Collection<AttributeProvider<?, ?, ?>> aps = attributeProviderAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .collect(Collectors.toList());
        for (AttributeProvider<?, ?, ?> ap : aps) {
            attributes.addAll(ap.getUserAttributes(subjectId));
        }

        return attributes;
    }

    public Collection<GrantedAuthority> fetchUserAuthorities(String subjectId, String realm)
        throws NoSuchUserException {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(Config.R_USER));

        // fetch all authorities for subject
        Collection<GrantedAuthority> userAuthorities = subjectService.getAuthorities(subjectId);
        authorities.addAll(userAuthorities);

        return authorities;
    }

    private Collection<RealmRole> fetchUserRealmRoles(String subjectId, String realm, Set<String> groupIds)
        throws NoSuchUserException {
        // merge directly assigned roles with those assigned to groups
        Set<RealmRole> roles = new HashSet<>();
        roles.addAll(roleService.getRoles(subjectId, realm));
        if (groupIds != null) {
            groupIds.forEach(groupId -> {
                roles.addAll(roleService.getRoles(groupId, realm));
            });
        }

        return roles;
    }

    public Collection<RealmRole> fetchUserRealmRoles(String subjectId, String realm) throws NoSuchUserException {
        // fetch groups to retrive roles associated
        Collection<Group> groups = fetchUserGroups(subjectId, realm);
        Set<String> groupIds = groups.stream().map(g -> g.getGroupId()).collect(Collectors.toSet());
        return fetchUserRealmRoles(subjectId, realm, groupIds);
    }

    public Collection<SpaceRole> fetchUserSpaceRoles(String subjectId, String realm) throws NoSuchUserException {
        // we don't filter space roles per realm, so read all
        return spaceRoleService.getRoles(subjectId);
    }

    public Collection<Group> fetchUserGroups(String subjectId, String realm) throws NoSuchUserException {
        return groupService.getSubjectGroups(subjectId, realm);
    }

    public void acceptTos(String subjectId) throws NoSuchUserException {
        userService.updateTos(subjectId, true);
    }

    public void rejectTos(String subjectId) throws NoSuchUserException {
        userService.updateTos(subjectId, false);
    }

    public void resetTos(String subjectId) throws NoSuchUserException {
        userService.updateTos(subjectId, null);
    }
}
