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

package it.smartcommunitylab.aac.apikey;

import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import it.smartcommunitylab.aac.dto.APIKey;
import it.smartcommunitylab.aac.manager.UserManager;

/**
 * @author raman
 *
 */
@Controller
public class APIKeyController {

	@Autowired
	private APIKeyManager keyManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;

	@GetMapping(value = "/resources/apikey/(apiKey:.*)")
	public @ResponseBody ResponseEntity<APIKey> findKey(@PathVariable String apiKey) {
		APIKey key = keyManager.findKey(apiKey);
		if (key != null && !key.hasExpired()) {
			return new ResponseEntity<APIKey>(key, HttpStatus.OK);
		}
		return new ResponseEntity<APIKey>(HttpStatus.NOT_FOUND);
	}
	
	
	/**
	 * Delete a specified API key
	 * @param apiKey
	 * @return
	 */
	@DeleteMapping(value = "/apikey/{apiKey:.*}")
	public @ResponseBody ResponseEntity<Void> deleteKey(HttpServletRequest request, @PathVariable String apiKey) {
		APIKey key = keyManager.findKey(apiKey);
		if (key != null) {
			try {
				String clientId = getClientId(request);
				userManager.checkClientIdOwnership(clientId);
				keyManager.deleteKey(apiKey);
			} catch (SecurityException e) {
				return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
			}
		}
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	/**
	 * Delete a specified API key
	 * @param apiKey
	 * @return
	 */
	@GetMapping(value = "/apikey")
	public @ResponseBody ResponseEntity<List<APIKey>> getKeys(HttpServletRequest request) {
			try {
				String clientId = getClientId(request);
				return new ResponseEntity<List<APIKey>>(keyManager.getClientKeys(clientId), HttpStatus.OK);
			} catch (SecurityException e) {
				return new ResponseEntity<List<APIKey>>(HttpStatus.UNAUTHORIZED);
			}
	}

	/**
	 * Delete a specified API key
	 * @param apiKey
	 * @return 
	 */
	@PutMapping(value = "/apikey/{apiKey:.*}")
	public @ResponseBody ResponseEntity<APIKey> updateKey(HttpServletRequest request, @RequestBody APIKey body, @PathVariable String apiKey) {
		APIKey key = keyManager.findKey(apiKey);
		if (key != null) {
			try {
				String clientId = getClientId(request);
				userManager.checkClientIdOwnership(clientId);
				if (body.getValidity() != null && body.getValidity() > 0) {
					keyManager.updateKeyValidity(apiKey, body.getValidity());
				}
				if (body.getAdditionalInformation() != null) {
					keyManager.updateKeyData(apiKey, body.getAdditionalInformation());
				}
				return new ResponseEntity<APIKey>(keyManager.findKey(apiKey), HttpStatus.UNAUTHORIZED);
			} catch (SecurityException e) {
				return new ResponseEntity<APIKey>(HttpStatus.UNAUTHORIZED);
			}
		}
		return new ResponseEntity<APIKey>(HttpStatus.NOT_FOUND);
	}	
	/**
	 * Create an API key with the specified properties (validity and additional info)
	 * @param apiKey
	 * @return created entity
	 */
	@PostMapping(value = "/apikey")
	public @ResponseBody ResponseEntity<APIKey> createKey(HttpServletRequest request, @RequestBody APIKey body) {
		try {
			String clientId = getClientId(request);
			APIKey keyObj = keyManager.createKey(clientId, body.getValidity(), body.getAdditionalInformation());
			return new ResponseEntity<APIKey>(keyObj, HttpStatus.UNAUTHORIZED);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<APIKey>(HttpStatus.NOT_FOUND);
		}
	}	


	private String getClientId(HttpServletRequest request) {
		try {
			String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
			if (parsedToken == null) throw new SecurityException("No clientId specified");
			OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
			if (auth == null || auth.getUserAuthentication() != null) throw new SecurityException("Invalid token");
			String clientId = auth.getOAuth2Request().getClientId();
			return clientId;
		} catch (Exception e) {
			throw new SecurityException(e.getMessage());
		}
	}
	
	
}
