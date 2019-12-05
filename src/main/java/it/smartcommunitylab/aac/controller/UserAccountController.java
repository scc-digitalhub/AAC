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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import it.smartcommunitylab.aac.dto.AccountProfile;
import it.smartcommunitylab.aac.dto.BasicProfile;
import it.smartcommunitylab.aac.manager.BasicProfileManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.oauth.OAuthProviders;
import it.smartcommunitylab.aac.oauth.OAuthProviders.ClientResources;

/**
 * @author raman
 *
 */
@Controller
public class UserAccountController {

	@Autowired
	private UserManager userManager;
	@Autowired
	private BasicProfileManager profileManager;
	@Autowired
	private OAuthProviders providers;
	

	@GetMapping("/account/profile")
	public ResponseEntity<BasicProfile> readProfile() {
		Long user = userManager.getUserId();
		if (user == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return ResponseEntity.ok(profileManager.getBasicProfileById(user.toString()));
	}
	@GetMapping("/account/accounts")
	public ResponseEntity<AccountProfile> getAccounts() {
		Long user = userManager.getUserId();
		if (user == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return ResponseEntity.ok(profileManager.getAccountProfileById(user.toString()));
	}
	
	@GetMapping("/account/providers")
	public ResponseEntity<List<String>> getProviders() {
		return ResponseEntity.ok(providers.getProviders().stream().map(ClientResources::getProvider).collect(Collectors.toList()));
	}

	
	
	// READ Accounts
	// MANAGE accounts: add/merge, delete

	// REMOVE account
	
	// READ 3rd-party app authorizations
	// MANAGE 3rd-party apps: revoke authorizations 
	
	// TODO: privacy policies
}
