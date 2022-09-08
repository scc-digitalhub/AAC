package it.smartcommunitylab.aac.controller;

import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.dto.UserEmail;
import it.smartcommunitylab.aac.dto.UserStatus;
import it.smartcommunitylab.aac.dto.UserSubject;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.model.SubjectStatus;

/*
 * Base controller for users
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseUserController implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected UserManager userManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(userManager, "user manager is required");
    }

    @Autowired
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
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
            @RequestParam(required = false) String q, Pageable pageRequest)
            throws NoSuchRealmException {
        logger.debug("list users for realm {}",
                StringUtils.trimAllWhitespace(realm));

        // list users owned or accessible by this realm
        return userManager.searchUsers(realm, q, pageRequest);
    }

    @GetMapping("/users/{realm}/{userId}")
    @Operation(summary = "fetch a specific user from realm")
    public User getUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        // get the user as visible from the given realm
        logger.debug("get user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        return userManager.getUser(realm, userId);
    }

    @DeleteMapping("/users/{realm}/{userId}")
    @Operation(summary = "delete a specific user from realm")
    public void deleteUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        // remove user for this realm, will delete if owned
        logger.debug("delete user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        userManager.removeUser(realm, userId);
    }

    @PostMapping("/users/{realm}")
    @Operation(summary = "create a new user in realm")
    public User createUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull UserSubject reg) throws NoSuchRealmException {
        logger.debug("register user for realm {}",
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("registration bean " + StringUtils.trimAllWhitespace(reg.toString()));
        }

        // create a user without identities or accounts
        // TODO
        return null;
    }

    @PutMapping("/users/{realm}")
    @Operation(summary = "invite a new user in realm")
    public User inviteUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull UserEmail reg)
            throws NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("invite user {} for realm {}", StringUtils.trimAllWhitespace(reg.getEmail()),
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("registration bean " + StringUtils.trimAllWhitespace(reg.toString()));
        }

        // invite a user
        User u = userManager.inviteUser(realm, reg.getEmail());

        // request verify for the user identity
        u.getIdentities().stream()
                .filter(i -> SystemKeys.AUTHORITY_INTERNAL.equals(i.getAuthority()))
                .forEach(i -> {
                    if (!i.getAccount().isEmailVerified()) {
                        try {
                            i = userManager.verifyUserIdentity(realm, u.getSubjectId(), i.getProvider(), i.getId());
                        } catch (RegistrationException | NoSuchRealmException | NoSuchUserException
                                | NoSuchProviderException e) {
                            // skip
                            logger.error("error verifying invited user {}:{} {} for realm {}",
                                    u.getSubjectId(), i.getId(),
                                    StringUtils.trimAllWhitespace(reg.getEmail()),
                                    StringUtils.trimAllWhitespace(realm));
                        }
                    }
                });

        return u;
    }

    @PutMapping("/users/{realm}/{userId}/status")
    @Operation(summary = "update the status of a specific user in realm")
    public User updateUserStatus(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid @NotNull UserStatus reg)
            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("update status for user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

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
     * User identities
     * 
     */
    @PostMapping("/users/{realm}/{userId}/identity")
    @Operation(summary = "add a new identity to a specific user in realm")
    public UserIdentity createUserIdentity(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid @NotNull AbstractIdentity reg)
            throws NoSuchRealmException, RegistrationException, NoSuchUserException, NoSuchProviderException {
        logger.debug("register user for realm {}",
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("registration bean {}", StringUtils.trimAllWhitespace(String.valueOf(reg)));
        }

        // extract from model
        String provider = reg.getProvider();
        return userManager.createUserIdentity(realm, userId, provider, reg);
    }

    @GetMapping("/users/{realm}/{userId}/identity")
    @Operation(summary = "list identities for a specific user in realm")
    public Collection<UserIdentity> getUserIdentity(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchUserException, NoSuchRealmException {
        logger.debug("get identities for user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));
        // fetch from user
        // TODO refactor
        User user = userManager.getUser(realm, userId);
        Collection<UserIdentity> identities = user.getIdentities();
        identities.forEach(identity -> {
            // clear credentials if loaded
            if (identity instanceof CredentialsContainer) {
                ((CredentialsContainer) identity).eraseCredentials();
            }
        });

        return identities;
    }

    @GetMapping("/users/{realm}/{userId}/identity/{identityUuid}")
    @Operation(summary = "get a specific identity from a specific user in realm")
    public UserIdentity getUserIdentity(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identityUuid)
            throws NoSuchUserException, NoSuchRealmException {
        logger.debug("get user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));
        // fetch from user
        // TODO refactor
        User user = userManager.getUser(realm, userId);
        UserIdentity identity = user.getIdentities().stream().filter(i -> i.getUuid().equals(identityUuid)).findFirst()
                .orElse(null);

        if (identity == null) {
            throw new NoSuchUserException();
        }

        // clear credentials if loaded
        if (identity instanceof CredentialsContainer) {
            ((CredentialsContainer) identity).eraseCredentials();
        }

        return identity;

    }

    @PutMapping("/users/{realm}/{userId}/identity/{identityUuid}")
    @Operation(summary = "update a specific identity for a specific user in realm")
    public UserIdentity updateUserIdentity(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identityUuid,
            @RequestBody @Valid @NotNull AbstractIdentity reg)
            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("update user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        // extract from model
        String provider = reg.getProvider();
        String identityId = reg.getId();
        return userManager.updateUserIdentity(realm, userId, provider, identityId, reg);
    }

    @DeleteMapping("/users/{realm}/{userId}/identity/{identityUuid}")
    @Operation(summary = "delete a specific identity from a specific user in realm")
    public void deleteUserIdentity(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identityUuid)
            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("update user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        // fetch from user
        // TODO refactor
        User user = userManager.getUser(realm, userId);
        UserIdentity identity = user.getIdentities().stream().filter(i -> i.getUuid().equals(identityUuid)).findFirst()
                .orElse(null);

        if (identity == null) {
            throw new NoSuchUserException();
        }

        // extract from model
        String provider = identity.getProvider();
        String identityId = identity.getId();

        userManager.deleteUserIdentity(realm, userId, provider, identityId);
    }

    @PutMapping("/users/{realm}/{userId}/identity/{identityUuid}/confirm")
    @Operation(summary = "confirm an identity for a given user in realm")
    public UserIdentity confirmUserIdentity(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identityUuid)
            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("confirm identity {} for user {} for realm {}", StringUtils.trimAllWhitespace(identityUuid),
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));
        // fetch from user
        // TODO refactor
        User user = userManager.getUser(realm, userId);
        UserIdentity identity = user.getIdentities().stream().filter(i -> i.getUuid().equals(identityUuid)).findFirst()
                .orElse(null);

        if (identity == null) {
            throw new NoSuchUserException();
        }

        // extract from model
        String provider = identity.getProvider();
        String identityId = identity.getId();

        return userManager.confirmUserIdentity(realm, userId, provider, identityId);
    }

    @DeleteMapping("/users/{realm}/{userId}/identity/{identityUuid}/confirm")
    @Operation(summary = "unconfirm an identity for a given user in realm")
    public UserIdentity unconfirmUserIdentity(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identityUuid)
            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("unconfirm identity {} for user {} for realm {}", StringUtils.trimAllWhitespace(identityUuid),
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));
        // fetch from user
        // TODO refactor
        User user = userManager.getUser(realm, userId);
        UserIdentity identity = user.getIdentities().stream().filter(i -> i.getUuid().equals(identityUuid)).findFirst()
                .orElse(null);

        if (identity == null) {
            throw new NoSuchUserException();
        }

        // extract from model
        String provider = identity.getProvider();
        String identityId = identity.getId();

        return userManager.unconfirmUserIdentity(realm, userId, provider, identityId);
    }

    @PostMapping("/users/{realm}/{userId}/identity/{identityUuid}/confirm")
    @Operation(summary = "verify an identity for a given user in realm")
    public UserIdentity verifyUserIdentity(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identityUuid)
            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("verify identity {} for user {} for realm {}", StringUtils.trimAllWhitespace(identityUuid),
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));
        // fetch from user
        // TODO refactor
        User user = userManager.getUser(realm, userId);
        UserIdentity identity = user.getIdentities().stream().filter(i -> i.getUuid().equals(identityUuid)).findFirst()
                .orElse(null);

        if (identity == null) {
            throw new NoSuchUserException();
        }

        // extract from model
        String provider = identity.getProvider();
        String identityId = identity.getId();

        return userManager.verifyUserIdentity(realm, userId, provider, identityId);
    }

    @PutMapping("/users/{realm}/{userId}/identity/{identityUuid}/status")
    @Operation(summary = "update status for an identity for a given user in realm")
    public UserIdentity updateUserIdentityStatus(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identityUuid,
            @RequestBody @Valid @NotNull UserStatus reg)
            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("update status for user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        // fetch from user
        // TODO refactor
        User user = userManager.getUser(realm, userId);
        UserIdentity identity = user.getIdentities().stream().filter(i -> i.getUuid().equals(identityUuid)).findFirst()
                .orElse(null);

        if (identity == null) {
            throw new NoSuchUserException();
        }

        // extract from model
        String provider = identity.getProvider();
        String identityId = identity.getId();

        SubjectStatus status = reg.getStatus();
        if (SubjectStatus.LOCKED == status) {
            return userManager.lockUserIdentity(realm, userId, provider, identityId);
        } else if (SubjectStatus.ACTIVE == status) {
            return userManager.unlockUserIdentity(realm, userId, provider, identityId);
        }

        throw new IllegalArgumentException("invalid_status");
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
