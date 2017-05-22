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

package it.smartcommunitylab.aac.wso2;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.keymanager.model.AACService;
import it.smartcommunitylab.aac.model.ClientAppBasic;

@Controller
public class WSO2Controller {

//	@Autowired
//	private UserRepository userRepository;
//	@Autowired
//	private ClientDetailsRepository clientDetailsRepository;
//	@Autowired
//	private ResourceRepository resourceRepository;
//	@Autowired
//	private APIProviderManager providerManager;
//	@Autowired
//	private ClientDetailsManager clientDetailsManager;	
//	@Autowired
//	private RegistrationRepository registrationRepository;
//	@Autowired
//	private ResourceManager resourceManager;
	
	@Autowired
	private WSO2Manager wso2Manager;
	
	@Autowired
	private TokenStore tokenStore;	
	
	private static final Log logger = LogFactory.getLog(WSO2Controller.class);
	
	@RequestMapping(value = "/wso2/client/{userName:.+}", method=RequestMethod.POST)
	public @ResponseBody ClientAppBasic createClient(HttpServletResponse response, @RequestBody ClientAppBasic app, @PathVariable("userName") String userName) throws Exception {
		try {
		ClientAppBasic resApp = wso2Manager.createClient(app, userName);
		
		if (resApp == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		
		return resApp;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return null;
		}
	}
	
	@RequestMapping(value = "/wso2/client/{clientId}", method=RequestMethod.PUT)
	public @ResponseBody ClientAppBasic updateClient(HttpServletResponse response, @RequestBody ClientAppBasic app, @PathVariable("clientId") String clientId) throws Exception {
		try {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		ClientAppBasic resApp = wso2Manager.updateClient(clientId, app);
		
		return resApp;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return null;
		}
	}	
	
	@RequestMapping(value = "/wso2/client/validity/{clientId}/{validity}", method=RequestMethod.PATCH)
	public @ResponseBody void updateTokenValidity(HttpServletResponse response, @PathVariable("clientId") String clientId, @PathVariable("validity") Integer validity) throws Exception {
		try {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		wso2Manager.updateValidity(clientId, validity);
		
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}	
	
	@RequestMapping(value = "/wso2/client/scope/{clientId}", method=RequestMethod.POST)
	public @ResponseBody void updateClientScope(HttpServletResponse response, @PathVariable("clientId") String clientId, @RequestParam String scope) throws Exception {
		try {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		wso2Manager.updateClientScope(clientId, scope);
		
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}	
	
	
	@RequestMapping(value = "/wso2/client/{clientId}", method = RequestMethod.GET)
	public @ResponseBody ClientAppBasic getClient(HttpServletResponse response, @PathVariable("clientId") String clientId) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		try {
			ClientAppBasic resApp = wso2Manager.getClient(clientId);

			if (resApp == null) {
				response.setStatus(HttpStatus.NOT_FOUND.value());
				return null;
			}

			response.setStatus(HttpStatus.OK.value());

			return resApp;

		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return null;
		}
	}

	@RequestMapping(value = "/wso2/client/{clientId}", method = RequestMethod.DELETE)
	public @ResponseBody void deleteClient(HttpServletResponse response, @PathVariable("clientId") String clientId) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		try {
			wso2Manager.deleteClient(clientId);

			response.setStatus(HttpStatus.OK.value());
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}	
	
	@RequestMapping("/wso2/client/token_revoke/{token}")
	public @ResponseBody
	String revokeToken(@PathVariable String token) {
		OAuth2AccessToken accessTokenObj = tokenStore.readAccessToken(token);
		if (accessTokenObj != null) {
			if (accessTokenObj.getRefreshToken() != null) {
				tokenStore.removeRefreshToken(accessTokenObj.getRefreshToken());
			}
			tokenStore.removeAccessToken(accessTokenObj);
		}
		return "";
	}		
	
	
	@RequestMapping(value = "/wso2/resources/{userName:.+}", method = RequestMethod.POST)
	public @ResponseBody void createResources(HttpServletResponse response, @RequestBody AACService service, @PathVariable("userName") String userName) throws Exception {
		try {
			
			String un = userName.replace("-AT-", "@");
			un = Utils.extractUserFromTenant(un);
			
			boolean ok = wso2Manager.createResource(service, un);

			if (!ok) {
				response.setStatus(HttpStatus.BAD_REQUEST.value());
			}

		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

	}

	@RequestMapping(value = "/wso2/resources/{resourceName:.+}", method = RequestMethod.DELETE)
	public @ResponseBody void deleteResources(HttpServletResponse response, @PathVariable("resourceName") String resourceName) throws Exception {
		try {
			
			
			
			wso2Manager.deleteResource(resourceName);
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

	}
	
	
}
