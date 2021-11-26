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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.dto.AttributesRegistrationDTO;
import it.smartcommunitylab.aac.model.User;

/*
 * Base controller for users
 */
@PreAuthorize("hasAuthority(this.authority)")
public class BaseUserController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserManager userManager;

    public String getAuthority() {
        return Config.R_USER;
    }

    @GetMapping("/user/{realm}")
    public Collection<User> listUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list users for realm {}",
                StringUtils.trimAllWhitespace(realm));

        // list users owned or accessible by this realm
        return userManager.listUsers(realm);
    }

    @GetMapping("/user/{realm}/{userId}")
    public User getUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        // get the user as visible from the given realm
        logger.debug("get user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        return userManager.getUser(realm, userId);
    }

    @DeleteMapping("/user/{realm}/{userId}")
    public void deleteUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        // remove user for this realm, will delete if owned
        logger.debug("delete user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        userManager.removeUser(realm, userId);
    }

    /*
     * User management
     * 
     */
    @PostMapping("/user/{realm}")
    public User registerUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull UserAccount account) throws NoSuchRealmException {
        logger.debug("register user for realm {}",
                StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("registration bean " + StringUtils.trimAllWhitespace(account.toString()));
        }

        return null;
    }

    // TODO evaluate, are UserAccounts editable?
    @PutMapping("/user/{realm}/{userId}")
    public User updateUser(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid @NotNull UserAccount account) throws NoSuchClientException, NoSuchRealmException {
        logger.debug("update user {} for realm {}",
                StringUtils.trimAllWhitespace(userId), StringUtils.trimAllWhitespace(realm));

        return null;
    }

    /*
     * User Attributes management
     * 
     * TODO
     */

    @GetMapping("/user/{realm}/{userId}/attributes")
    public Collection<UserAttributes> getRealmUserAttributes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        logger.debug("get attributes for user {} for realm {}",
                StringUtils.trimAllWhitespace(subjectId), StringUtils.trimAllWhitespace(realm));

        Collection<UserAttributes> attributes = userManager.getUserAttributes(realm, subjectId);
        return attributes;
    }

    @PostMapping("/user/{realm}/{userId}/attributes")
    public UserAttributes addRealmUserAttributes(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @RequestBody @Valid @NotNull AttributesRegistrationDTO reg)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {
        logger.debug("update attributes for user {} for realm {}",
                StringUtils.trimAllWhitespace(subjectId), StringUtils.trimAllWhitespace(realm));

        // extract registration
        String identifier = reg.getIdentifier();
        String provider = reg.getProvider();
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("a valid provider is required");
        }

        if (reg.getAttributes() == null) {
            throw new IllegalArgumentException("attributes can not be null");
        }

        Map<String, Serializable> attributes = reg.getAttributes().stream()
                .filter(a -> a.getValue() != null)
                .collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));

        // register
        UserAttributes ua = userManager.setUserAttributes(realm, subjectId, provider, identifier, attributes);
        return ua;
    }

}
