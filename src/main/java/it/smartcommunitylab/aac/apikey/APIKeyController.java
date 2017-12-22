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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.dto.APIKey;

/**
 * @author raman
 *
 */
@Controller
@Api(tags = { "AACApiKey" })
public class APIKeyController {

	private Log log = LogFactory.getLog(getClass());

	@Autowired
	private APIKeyManager keyManager;
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;

	@ApiOperation(value="Validate key")
	@GetMapping(value = "/apikeycheck/{apiKey:.*}")
	public @ResponseBody APIKey findKey(@PathVariable String apiKey) throws EntityNotFoundException {
		APIKey key = keyManager.findKey(apiKey);
		
		if (key != null && !key.hasExpired()) {
			return key;
		}
		throw new EntityNotFoundException();
	}
	
	
	/**
	 * Delete a specified API key
	 * @param apiKey
	 * @return
	 */
	@ApiOperation(value="Delete key")
	@DeleteMapping(value = "/apikey/{apiKey:.*}")
	public @ResponseBody void deleteKey(HttpServletRequest request, @PathVariable String apiKey) throws SecurityException {
		APIKey key = keyManager.findKey(apiKey);
		if (key != null) {
			String clientId = getClientId(request);
			if (!clientId.equals(key.getClientId())) {
				throw new SecurityException();
			}
			keyManager.deleteKey(apiKey);
		}
	}

	/**
	 * Delete a specified API key
	 * @param apiKey
	 * @return
	 */
	@ApiOperation(value="List keys")
	@GetMapping(value = "/apikey")
	public @ResponseBody List<APIKey> getKeys(HttpServletRequest request) throws SecurityException {
		String clientId = getClientId(request);
		return keyManager.getClientKeys(clientId);
	}

	/**
	 * Delete a specified API key
	 * @param apiKey
	 * @return 
	 */
	@ApiOperation(value="Update key")
	@PutMapping(value = "/apikey/{apiKey:.*}")
	public @ResponseBody APIKey updateKey(HttpServletRequest request, @RequestBody APIKey body, @PathVariable String apiKey) throws SecurityException, EntityNotFoundException {
		APIKey key = keyManager.findKey(apiKey);
		if (key != null) {
			String clientId = getClientId(request);
			if (!clientId.equals(key.getClientId())) {
				throw new SecurityException();
			}

			if (body.getValidity() != null && body.getValidity() > 0) {
				keyManager.updateKeyValidity(apiKey, body.getValidity());
			}
			if (body.getAdditionalInformation() != null) {
				keyManager.updateKeyData(apiKey, body.getAdditionalInformation());
			}
			return keyManager.findKey(apiKey);
		}
		throw new EntityNotFoundException();
	}	
	/**
	 * Create an API key with the specified properties (validity and additional info)
	 * @param apiKey
	 * @return created entity
	 */
	@ApiOperation(value="Create key")
	@PostMapping(value = "/apikey")
	public @ResponseBody APIKey createKey(HttpServletRequest request, @RequestBody APIKey body) throws EntityNotFoundException {
		String clientId = getClientId(request);
		APIKey keyObj = keyManager.createKey(clientId, body.getValidity(), body.getAdditionalInformation(), body.getScope());
		return keyObj;
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
	
	@ExceptionHandler(EntityNotFoundException.class)
	@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Key or client does not exist")
	public void notFound() {
	}

	@ExceptionHandler(SecurityException.class)
	@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Operation not permitted")
	public void unauthorized() {
	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	@ResponseBody
	public void handleBadRequest(HttpServletRequest req, Exception ex) {
		log.error("Error processing API Key operation", ex);
	}
	
}
