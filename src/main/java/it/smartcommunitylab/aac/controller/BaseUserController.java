package it.smartcommunitylab.aac.controller;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.dto.AttributesRegistrationDTO;
import it.smartcommunitylab.aac.dto.UserEmailBean;
import it.smartcommunitylab.aac.dto.UserStatusBean;
import it.smartcommunitylab.aac.dto.UserSubjectBean;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.model.UserStatus;

/*
 * Base controller for users
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseUserController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected UserManager userManager;

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Users
     */

    @GetMapping("/users/{realm}")
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
    public User createUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull UserSubjectBean reg) throws NoSuchRealmException {
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
    public User inviteUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull UserEmailBean reg)
            throws NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("invite user {} for realm {}", StringUtils.trimAllWhitespace(reg.getEmail()),
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("registration bean " + StringUtils.trimAllWhitespace(reg.toString()));
        }

        // invite a user
        return userManager.inviteUser(realm, reg.getEmail());
    }

    @PutMapping("/users/{realm}/{userId}/status")
    public User updateUserStatus(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid @NotNull UserStatusBean reg)
            throws NoSuchUserException, NoSuchRealmException, RegistrationException, NoSuchProviderException {
        logger.debug("update status for user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        // extract from model
        UserStatus status = reg.getStatus();
        if (UserStatus.BLOCKED == status) {
            return userManager.blockUser(realm, userId);
        } else if (UserStatus.ACTIVE == status) {
            return userManager.activateUser(realm, userId);
        } else if (UserStatus.INACTIVE == status) {
            return userManager.inactivateUser(realm, userId);
        }

        throw new IllegalArgumentException("invalid_status");
    }

    /*
     * User identities
     * 
     */
    @PostMapping("/users/{realm}/{userId}/identity")
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
    public UserIdentity updateUserIdentityStatus(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identityUuid,
            @RequestBody @Valid @NotNull UserStatusBean reg)
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

        UserStatus status = reg.getStatus();
        if (UserStatus.LOCKED == status) {
            return userManager.lockUserIdentity(realm, userId, provider, identityId);
        } else if (UserStatus.ACTIVE == status) {
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
