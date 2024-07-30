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

package it.smartcommunitylab.aac.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.accounts.model.EditableUserAccount;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.dto.UserEmail;
import it.smartcommunitylab.aac.dto.UserStatus;
import it.smartcommunitylab.aac.dto.UserSubject;
import it.smartcommunitylab.aac.groups.GroupManager;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.users.UserManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * Base controller for users
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseUserController implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected UserManager userManager;
    protected GroupManager groupManager;
    protected RealmRoleManager roleManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(userManager, "user manager is required");
    }

    @Autowired
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    @Autowired
    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Autowired
    public void setRoleManager(RealmRoleManager roleManager) {
        this.roleManager = roleManager;
    }
    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Users
     */

    @GetMapping("/users/{realm}")
    @Operation(summary = "list users from realm")
    public Page<User> listUser(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String role,
        Pageable pageRequest
    ) throws NoSuchRealmException, NoSuchGroupException {
        logger.debug("list users for realm {}", StringUtils.trimAllWhitespace(realm));

        if(group != null) {
            //list members for group
            String[] userIds = groupManager.getGroupMembers(realm, group).toArray(new String[0]);
            
            List<User> users = new ArrayList<>();
            for(int i = 0; i< userIds.length; i++) {
                if(pageRequest == null || (i >= pageRequest.getOffset() && i <=pageRequest.getOffset()+pageRequest.getPageSize())) {
                    try {
                        users.add(userManager.getUser(realm, userIds[i]));
                    } catch (NoSuchUserException nue) {
                        //skip, the id may refer to another entity
                    }
                }
            }

            return new PageImpl<>(users, pageRequest, userIds.length);
        }

        // list users owned or accessible by this realm
        return userManager.searchUsers(realm, q, pageRequest);
    }

    @GetMapping("/users/{realm}/{userId}")
    @Operation(summary = "fetch a specific user from realm")
    public User getUser(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId
    ) throws NoSuchRealmException, NoSuchUserException {
        // get the user as visible from the given realm
        logger.debug(
            "get user {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.getUser(realm, userId);
    }

    @DeleteMapping("/users/{realm}/{userId}")
    @Operation(summary = "delete a specific user from realm")
    public void deleteUser(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId
    ) throws NoSuchRealmException, NoSuchUserException {
        // remove user for this realm, will delete if owned
        logger.debug(
            "delete user {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(realm)
        );

        userManager.removeUser(realm, userId);
    }

    @PostMapping("/users/{realm}")
    @Operation(summary = "create a new user in realm")
    public User createUser(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull UserSubject reg
    ) throws NoSuchRealmException {
        logger.debug("register user for realm {}", StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("registration bean {}", StringUtils.trimAllWhitespace(reg.toString()));
        }

        // create a user without identities or accounts
        // TODO
        return null;
    }

    @PutMapping("/users/{realm}")
    @Operation(summary = "invite a new user in realm")
    public User inviteUser(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull UserEmail reg
    ) throws NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "invite user {} for realm {}",
            StringUtils.trimAllWhitespace(reg.getEmail()),
            StringUtils.trimAllWhitespace(realm)
        );

        if (logger.isTraceEnabled()) {
            logger.trace("registration bean {}", StringUtils.trimAllWhitespace(reg.toString()));
        }

        // invite a user
        User u = userManager.inviteUser(realm, null, reg.getEmail());
        return u;
    }

    @PutMapping("/users/{realm}/{userId}/status")
    @Operation(summary = "update the status of a specific user in realm")
    public User updateUserStatus(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @RequestBody @Valid @NotNull UserStatus reg
    ) throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug(
            "update status for user {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(realm)
        );

        // extract from model
        SubjectStatus status = reg.getStatus();
        if (SubjectStatus.BLOCKED == status) {
            return userManager.blockUser(realm, userId);
        } else if (SubjectStatus.ACTIVE == status) {
            return userManager.activateUser(realm, userId);
        } else if (SubjectStatus.INACTIVE == status) {
            return userManager.inactivateUser(realm, userId);
        }

        throw new IllegalArgumentException("invalid_status");
    }

    /*
     * User accounts
     *
     */
    @PostMapping("/users/{realm}/{userId}/account")
    @Operation(summary = "add a new account to a specific user in realm")
    public UserAccount createUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @RequestBody @Valid @NotNull AbstractUserAccount reg
    )
        throws NoSuchRealmException, RegistrationException, NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("register user for realm {}", StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("registration bean {}", StringUtils.trimAllWhitespace(String.valueOf(reg)));
        }

        // extract provider from reg
        // NOTE: authority could be incorrect due to conversion from AbstractAccount
        String provider = reg.getProvider();

        return userManager.createUserAccount(realm, provider, userId, null, reg);
    }

    @GetMapping("/users/{realm}/{userId}/account")
    @Operation(summary = "list accounts for a specific user in realm")
    public Collection<UserAccount> listUserAccounts(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId
    ) throws NoSuchUserException, NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "get accounts for user {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.listUserAccounts(realm, userId);
    }

    @GetMapping("/users/{realm}/{userId}/account/{uuid}")
    @Operation(summary = "get a specific account from a specific user in realm")
    public UserAccount getUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid
    ) throws NoSuchUserException, NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "get user {} account {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.getUserAccount(realm, userId, uuid);
    }

    @PutMapping("/users/{realm}/{userId}/account/{uuid}")
    @Operation(summary = "update a specific account for a specific user in realm")
    public UserAccount updateUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
        @RequestBody @Valid @NotNull AbstractUserAccount reg
    )
        throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "update user {} account {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.updateUserAccount(realm, userId, uuid, reg);
    }

    @DeleteMapping("/users/{realm}/{userId}/account/{uuid}")
    @Operation(summary = "delete a specific account from a specific user in realm")
    public void deleteUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid
    )
        throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "update user {} account {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(realm)
        );

        userManager.deleteUserAccount(realm, userId, uuid);
    }

    //    @GetMapping("/users/{realm}/{userId}/account/{uuid}/edit")
    //    @Operation(summary = "get a specific editable account from a specific user in realm")
    //    public EditableUserAccount getEditableUserAccount(
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid)
    //            throws NoSuchUserException, NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException {
    //        logger.debug("get user {} edit account {} for realm {}", StringUtils.trimAllWhitespace(userId),
    //                StringUtils.trimAllWhitespace(uuid), StringUtils.trimAllWhitespace(realm));
    //
    //        return userManager.getEditableUserAccount(realm, userId, uuid);
    //    }
    //
    //    @PutMapping("/users/{realm}/{userId}/account/{uuid}/edit")
    //    @Operation(summary = "update a specific editable account for a specific user in realm")
    //    public EditableUserAccount updateEditableUserAccount(
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
    //            @RequestBody @Valid @NotNull AbstractEditableAccount reg)
    //            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException,
    //            NoSuchAuthorityException {
    //        logger.debug("update user {} editable account {} for realm {}", StringUtils.trimAllWhitespace(userId),
    //                StringUtils.trimAllWhitespace(uuid), StringUtils.trimAllWhitespace(realm));
    //
    //        return userManager.editUserAccount(realm, userId, uuid, reg);
    //    }

    @PutMapping("/users/{realm}/{userId}/account/{uuid}/confirm")
    @Operation(summary = "confirm an account for a given user in realm")
    public UserAccount confirmUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid
    )
        throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "confirm user {} account {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.confirmUserAccount(realm, userId, uuid);
    }

    @DeleteMapping("/users/{realm}/{userId}/account/{uuid}/confirm")
    @Operation(summary = "unconfirm an account for a given user in realm")
    public UserAccount unconfirmUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid
    )
        throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "unconfirm user {} account {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.unconfirmUserAccount(realm, userId, uuid);
    }

    @PostMapping("/users/{realm}/{userId}/account/{uuid}/confirm")
    @Operation(summary = "verify an account for a given user in realm")
    public UserAccount verifyUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid
    )
        throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "verify user {} account {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.verifyUserAccount(realm, userId, uuid);
    }

    @PutMapping("/users/{realm}/{userId}/account/{uuid}/lock")
    @Operation(summary = "lock an account for a given user in realm")
    public UserAccount lockUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid
    )
        throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "lock user {} account {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.lockUserAccount(realm, userId, uuid);
    }

    @DeleteMapping("/users/{realm}/{userId}/account/{uuid}/lock")
    @Operation(summary = "unlock an account for a given user in realm")
    public UserAccount unlockUserAccount(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid
    )
        throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "unlock user {} account {} for realm {}",
            StringUtils.trimAllWhitespace(userId),
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(realm)
        );

        return userManager.unlockUserAccount(realm, userId, uuid);
    }
    /*
     * User Attributes management
     *
     * TODO
     */

    //    @GetMapping("/users/{realm}/{userId}/attributes")
    //    public Collection<UserAttributes> getUserAttributes(
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
    //            throws NoSuchRealmException, NoSuchUserException {
    //        logger.debug("get attributes for user {} for realm {}",
    //                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));
    //
    //        Collection<UserAttributes> attributes = userManager.getUserAttributes(realm, userId);
    //        return attributes;
    //    }
    //
    //    @PostMapping("/users/{realm}/{userId}/attributes")
    //    public UserAttributes addUserAttributes(
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
    //            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
    //            @RequestBody @Valid @NotNull AttributesRegistrationDTO reg)
    //            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {
    //        logger.debug("update attributes for user {} for realm {}",
    //                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));
    //
    //        // extract registration
    //        String identifier = reg.getIdentifier();
    //        String provider = reg.getProvider();
    //        if (!StringUtils.hasText(provider)) {
    //            throw new IllegalArgumentException("a valid provider is required");
    //        }
    //
    //        if (reg.getAttributes() == null) {
    //            throw new IllegalArgumentException("attributes can not be null");
    //        }
    //
    //        Map<String, Serializable> attributes = reg.getAttributes().stream()
    //                .filter(a -> a.getValue() != null)
    //                .collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));
    //
    //        // register
    //        UserAttributes ua = userManager.setUserAttributes(realm, userId, provider, identifier, attributes);
    //        return ua;
    //    }

}
