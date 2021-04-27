/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.dto.UserProfile;
import it.smartcommunitylab.aac.internal.InternalUserManager;
import it.smartcommunitylab.aac.profiles.ProfileManager;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

/**
 * Application controller for user UI: account
 * 
 * Should handle only "current" user operations
 * 
 * @author raman
 *
 */
@Controller
public class UserAccountController {

    @Autowired
    private AuthenticationHelper authHelper;
    
    @Autowired
    private UserManager userManager;

//    @Autowired
//    private InternalUserManager internalUserManager;

    @Autowired
    private ProfileManager profileManager;

    // TODO MANAGE accounts: add/merge, delete

    @RequestMapping("/")
    public ModelAndView home() {
        return new ModelAndView("redirect:/account");
    }

    @RequestMapping("/account")
    public ModelAndView account() {
        UserDetails user = authHelper.getUserDetails();

        Map<String, Object> model = new HashMap<String, Object>();
        String username = user.getUsername();
        model.put("username", username);
        return new ModelAndView("account", model);
    }
    
    @GetMapping("/account/profile")
    public ResponseEntity<UserDetails> myProfile() throws InvalidDefinitionException {
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(user);

    }

    @GetMapping("/account/accounts")
    public ResponseEntity<List<AccountProfile>> getAccounts() {
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Collection<UserIdentity> identities = user.getIdentities();
        return ResponseEntity.ok(identities.stream().map(i -> i.getAccount().toProfile()).collect(Collectors.toList()));
    }

    @DeleteMapping("/account/profile")
    public ResponseEntity<Void> deleteProfile() throws NoSuchUserException, NoSuchRealmException {
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userManager.removeUser(user.getRealm(), user.getSubjectId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/account/profile")
    public ResponseEntity<BasicProfile> updateProfile(@RequestBody UserProfile profile)
            throws InvalidDefinitionException {
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            // TODO use update, the current user must exists
            // TODO implement update in userManager
//            internalUserManager.updateOrCreateAccount(cur.getSubjectId(), cur.getRealm(), profile.getUsername(), profile.getPassword(), profile.getEmail(), profile.getName(), profile.getSurname(), profile.getLang(), Collections.emptySet());

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(profileManager.curBasicProfile());
    }

    @GetMapping("/account/attributes")
    public ResponseEntity<Collection<UserAttributes>> readAttributes() {
        Collection<UserAttributes> result = userManager.getMyAttributes();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/account/connections")
    public ResponseEntity<Collection<ConnectedAppProfile>> readConnectedApps() {
        Collection<ConnectedAppProfile> result = userManager.getMyConnectedApps();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/account/connections/{clientId}")
    public ResponseEntity<Collection<ConnectedAppProfile>> deleteConnectedApp(@PathVariable String clientId) {

        userManager.deleteMyConnectedApp(clientId);

        Collection<ConnectedAppProfile> result = userManager.getMyConnectedApps();
        return ResponseEntity.ok(result);
    }
}
