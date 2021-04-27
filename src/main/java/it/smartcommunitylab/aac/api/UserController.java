package it.smartcommunitylab.aac.api;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.model.User;

@RestController
@RequestMapping("api/users")
public class UserController {

    @Autowired
    private UserManager userManager;

    @GetMapping("{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Collection<User> listUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        // list users owned or accessible by this realm
        return userManager.listUsers(realm);
    }

    @GetMapping("{realm}/{userId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public User getUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        // get the user as visible from the given realm
        return userManager.getUser(realm, userId);
    }

    @DeleteMapping("{realm}/{userId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void deleteUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException {
        // remove user for this realm, will delete if owned
        userManager.removeUser(realm, userId);
    }

    /*
     * User management
     * 
     */
    @PostMapping("{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public User registerUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid UserAccount account) throws NoSuchRealmException {
        return null;
    }

    // TODO evaluate, are UserAccounts editable?
    @PutMapping("{realm}/{userId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public User updateUser(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId,
            @RequestBody @Valid UserAccount account) throws NoSuchClientException, NoSuchRealmException {
        return null;
    }

    /*
     * User Attributes management
     * 
     * TODO
     */

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
