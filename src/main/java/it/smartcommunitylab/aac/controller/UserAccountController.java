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
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.dto.ConnectedAppProfile;
import it.smartcommunitylab.aac.dto.UserProfile;
import it.smartcommunitylab.aac.profiles.ProfileManager;
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

    @Autowired
    private ProfileManager profileManager;

    // TODO MANAGE accounts: add/merge, delete

    @GetMapping("/account/profile")
    public ResponseEntity<BasicProfile> myProfile() throws InvalidDefinitionException {
        BasicProfile profile = profileManager.curBasicProfile();
        if (profile == null) {
            return ResponseEntity.notFound().build();

        }

        return ResponseEntity.ok(profile);

    }

//    @GetMapping("/account/accounts")
//    public ResponseEntity<AccountProfile> getAccounts() {
//        Long user = userManager.getUserId();
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }
//        return ResponseEntity.ok(profileManager.getAccountProfileById(user.toString()));
//    }
//
//    @GetMapping("/account/providers")
//    public ResponseEntity<List<String>> getProviders() {
//        return ResponseEntity
//                .ok(providers.getProviders().stream().map(ClientResources::getProvider).collect(Collectors.toList()));
//    }
//
//    @DeleteMapping("/account/profile")
//    public ResponseEntity<Void> deleteProfile() {
//        Long user = userManager.getUserId();
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }
//        userManager.deleteUser(user);
//        return ResponseEntity.ok().build();
//    }

//    @PostMapping("/account/profile")
//    public ResponseEntity<BasicProfile> updateProfile(@RequestBody UserProfile profile) {
//        Long user = userManager.getUserId();
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }
//        try {
//            userManager.updateProfile(user, profile);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//        return ResponseEntity.ok(profileManager.getBasicProfileById(user.toString()));
//    }

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
