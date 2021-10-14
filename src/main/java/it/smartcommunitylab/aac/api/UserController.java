package it.smartcommunitylab.aac.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiUsersScope;
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

@RestController
@RequestMapping("api/users")
@PreAuthorize("hasAuthority('SCOPE_" + ApiUsersScope.SCOPE + "')")
public class UserController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserManager userManager;

    @GetMapping("{realm}")
    public Collection<User> listUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        // list users owned or accessible by this realm
        logger.debug("list users for realm " + String.valueOf(realm));
        return userManager.listUsers(realm);
    }

    @GetMapping("{realm}/{userId}")
    public User getUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        // get the user as visible from the given realm
        logger.debug("get user " + String.valueOf(userId) + " for realm " + String.valueOf(realm));
        return userManager.getUser(realm, userId);
    }

    @DeleteMapping("{realm}/{userId}")
    public void deleteUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        // remove user for this realm, will delete if owned
        logger.debug("delete user " + String.valueOf(userId) + " for realm " + String.valueOf(realm));
        userManager.removeUser(realm, userId);
    }

    /*
     * User management
     * 
     */
    @PostMapping("{realm}")
    public User registerUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid UserAccount account) throws NoSuchRealmException {
        logger.debug("register user for realm " + String.valueOf(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("registration bean " + String.valueOf(account));
        }

        return null;
    }

    // TODO evaluate, are UserAccounts editable?
    @PutMapping("{realm}/{userId}")
    public User updateUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid UserAccount account) throws NoSuchClientException, NoSuchRealmException {
        logger.debug("update user " + String.valueOf(userId) + " for realm " + String.valueOf(realm));
        return null;
    }

    /*
     * User Attributes management
     * 
     * TODO
     */

    @GetMapping("{realm}/{userId}/attributes")
    public Collection<UserAttributes> getRealmUserAttributes(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId)
            throws NoSuchRealmException, NoSuchUserException {
        Collection<UserAttributes> attributes = userManager.getUserAttributes(realm, subjectId);
        return attributes;
    }

    @PostMapping("{realm}/{userId}/attributes")
    public UserAttributes addRealmUserAttributes(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subjectId,
            @RequestBody AttributesRegistrationDTO reg)
            throws NoSuchRealmException, NoSuchUserException, NoSuchProviderException, NoSuchAttributeSetException {

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

//    @GetMapping("{realm}/{userId}/attributes")
//    public Collection<UserAttributes> getUserAttributes(
//            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
//            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
//            throws NoSuchRealmException, NoSuchUserException {
//        // get the user attributes as visible from the given realm
//        return userManager.getUserAttributes(realm, userId);
//    }
//
//    // TODO evaluate, can we register attributes?
//    @PostMapping("{realm}/{userId}/attributes")
//    public User addUserAttributes(
//            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
//            @RequestBody @Valid UserAttributes attributes) throws NoSuchRealmException {
//
//        return null;
//    }
//
//    // TODO evaluate, can we update attributes?
//    @PostMapping("{realm}/{userId}/attributes/{setId}")
//    public User updateUserAttributes(
//            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
//            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
//            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId,
//            @RequestBody @Valid UserAttributes attributes) throws NoSuchClientException, NoSuchRealmException {
//        return null;
//    }

}
