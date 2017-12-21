/**
 *    Copyright 2012-2013 Trento RISE
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
 */

package it.smartcommunitylab.aac.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.apikey.APIKeyManager;
import it.smartcommunitylab.aac.dto.APIKey;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Response.RESPONSE;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for performing the basic operations over the 
 * client apps.
 * @author raman
 *
 */
@ApiIgnore
@Controller
public class AppController {

	private Log logger = LogFactory.getLog(getClass());
	
	@Autowired
	private ClientDetailsManager clientDetailsAdapter;
	@Autowired
	private UserManager userManager;
	@Autowired
	private APIKeyManager keyManager;

	/**
	 * Retrieve the with the user data: currently on the username is added.
	 * @return
	 */
	@RequestMapping("/")
	public ModelAndView home() {
		return new ModelAndView("redirect:/dev");
	}

	/**
	 * Retrieve the with the user data: currently on the username is added.
	 * @return
	 */
	@RequestMapping("/dev")
	public ModelAndView developer() {
		Map<String,Object> model = new HashMap<String, Object>();
		
		String username = userManager.getUserFullName();
		model.put("username",username);
		model.put("roles", userManager.getUserRoles());
		return new ModelAndView("index", model);
	}
	
	/**
	 * Read the 
	 * @return {@link Response} entity containing the list of client app {@link ClientAppBasic} descriptors
	 */
	@RequestMapping("/dev/apps")
	public @ResponseBody Response getAppList() {
		Response response = new Response();
		response.setResponseCode(RESPONSE.OK);
		try {
			// read all the apps associated to the signed user 
			List<ClientAppBasic> list = clientDetailsAdapter.getByDeveloperId(userManager.getUserId());
			response.setData(list);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			response.setResponseCode(RESPONSE.ERROR);
			response.setErrorMessage(e.getMessage());
		}
		return response;
	}
	
	/**
	 * Read the 
	 * @return {@link Response} entity containing the list of client app {@link ClientAppBasic} descriptors
	 */
	@RequestMapping("/dev/apps/{clientId}")
	public @ResponseBody Response getApp(@PathVariable String clientId) {
		Response response = new Response();
		response.setResponseCode(RESPONSE.OK);
		try {
			// read the app associated to the client
			ClientAppBasic app = clientDetailsAdapter.getByClientId(clientId);
			if (!app.getUserName().equals(userManager.getUserId().toString())) {
				response.setResponseCode(RESPONSE.ERROR);
				response.setErrorMessage("Unauthorized");				
			} else {
				response.setData(app);
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			response.setResponseCode(RESPONSE.ERROR);
			response.setErrorMessage(e.getMessage());
		}
		return response;
	}
	
	/**
	 * create a new client app given a container with the name only
	 * @param appData
	 * @return {@link Response} entity containing the stored app {@link ClientAppBasic} descriptor
	 * @throws Exception
	 */
	@RequestMapping(method=RequestMethod.POST,value="/dev/apps")
	public @ResponseBody Response saveEmpty(@RequestBody ClientAppBasic appData) throws Exception {
		Response response = new Response();
		response.setResponseCode(RESPONSE.OK);
		try {
			response.setData(clientDetailsAdapter.create(appData, userManager.getUserId()));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			response.setResponseCode(RESPONSE.ERROR);
			response.setErrorMessage(e.getMessage());
		}
		return response;
	}

	@RequestMapping(method=RequestMethod.POST,value="/dev/apps/{clientId}")
	public @ResponseBody Response resetClientData(@PathVariable String clientId,@RequestParam String reset) {
		return reset(clientId, "clientSecretMobile".equals(reset));
	}

	/**
	 * Reset clientId or client secret
	 * @param clientId
	 * @param resetClientSecretMobile true to reset clientSecretMobile, false to reset clientSecret
	 * @return {@link Response} entity containing the stored app {@link ClientAppBasic} descriptor
	 */
	protected Response reset(String clientId, boolean resetClientSecretMobile) {
		Response response = new Response();
		response.setResponseCode(RESPONSE.OK);
		try {
			userManager.checkClientIdOwnership(clientId);
			if (resetClientSecretMobile) {
				response.setData(clientDetailsAdapter.resetClientSecretMobile(clientId));
			} else {
				response.setData(clientDetailsAdapter.resetClientSecret(clientId));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			response.setResponseCode(RESPONSE.ERROR);
			response.setErrorMessage(e.getMessage());
		}
		return response;
	}

	/**
	 * Delete the specified app
	 * @param clientId
	 * @return {@link Response} entity containing the deleted app {@link ClientAppBasic} descriptor
	 */
	@RequestMapping(method=RequestMethod.DELETE,value="/dev/apps/{clientId}")
	public @ResponseBody Response delete(@PathVariable String clientId) {
		Response response = new Response();
		response.setResponseCode(RESPONSE.OK);
		try {
			userManager.checkClientIdOwnership(clientId);
			response.setData(clientDetailsAdapter.delete(clientId));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			response.setResponseCode(RESPONSE.ERROR);
			response.setErrorMessage(e.getMessage());
		}
		return response;
	}

	/**
	 * Update the client app
	 * @param data
	 * @param clientId
	 * @return {@link Response} entity containing the updated app {@link ClientAppBasic} descriptor
	 */
	@RequestMapping(method=RequestMethod.PUT,value="/dev/apps/{clientId}")
	public @ResponseBody Response update(@RequestBody ClientAppBasic data, @PathVariable String clientId) {
		Response response = new Response();
		response.setResponseCode(RESPONSE.OK);
		try {
			userManager.checkClientIdOwnership(clientId);
			response.setData(clientDetailsAdapter.update(clientId, data));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			response.setResponseCode(RESPONSE.ERROR);
			response.setErrorMessage(e.getMessage());
		}
		return response;
	}

	/**
	 * Delete a specified API key
	 * @param apiKey
	 * @return
	 */
	@DeleteMapping(value = "/dev/apikey/{clientId}/{apiKey:.*}")
	public @ResponseBody ResponseEntity<Void> deleteKey(@PathVariable String clientId, @PathVariable String apiKey) {
		APIKey key = keyManager.findKey(apiKey);
		if (key != null) {
			try {
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
	@PutMapping(value = "/dev/apikey/{clientId}/{apiKey:.*}")
	public @ResponseBody ResponseEntity<APIKey> updateKey(@RequestBody APIKey body, @PathVariable String clientId, @PathVariable String apiKey) {
		APIKey key = keyManager.findKey(apiKey);
		if (key != null) {
			try {
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
	@PostMapping(value = "/dev/apikey/{clientId}")
	public @ResponseBody ResponseEntity<APIKey> createKey(@RequestBody APIKey body, @PathVariable String clientId) {
		try {
			APIKey keyObj = keyManager.createKey(clientId, body.getValidity(), body.getAdditionalInformation());
			return new ResponseEntity<APIKey>(keyObj, HttpStatus.UNAUTHORIZED);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<APIKey>(HttpStatus.NOT_FOUND);
		}
	}	
	
	@GetMapping(value = "/dev/apikey/{clientId}")
	public @ResponseBody ResponseEntity<List<APIKey>> getClientKeys(@PathVariable String clientId) {
		return new ResponseEntity<List<APIKey>>(keyManager.getClientKeys(clientId), HttpStatus.OK);
	}	
}
