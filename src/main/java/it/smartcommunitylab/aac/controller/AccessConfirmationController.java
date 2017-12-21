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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.repository.ResourceRepository;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for retrieving the model for and displaying the confirmation page for access to a protected resource.
 * 
 */
@ApiIgnore
@Controller
@SessionAttributes("authorizationRequest")
public class AccessConfirmationController {

	private static Log logger = LogFactory.getLog(AccessConfirmationController.class);
	
	@Autowired
	private ClientDetailsService clientDetailsService;
	@Autowired
	private ResourceRepository resourceRepository;

	/**
	 * Request the user confirmation for the resources enabled for the requesting client
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/oauth/confirm_access")
	public ModelAndView getAccessConfirmation(Map<String, Object> model) throws Exception {
		AuthorizationRequest clientAuth = (AuthorizationRequest) model.remove("authorizationRequest");
		// load client information given the client credentials obtained from the request
		ClientDetails client = clientDetailsService.loadClientByClientId(clientAuth.getClientId());
		ClientAppInfo info = ClientAppInfo.convert(client.getAdditionalInformation());
		List<Resource> resources = new ArrayList<Resource>();
		
		Set<String> all = client.getScope();
		Set<String> requested = clientAuth.getScope();
		if (requested == null || requested.isEmpty()) {
			requested = all;
		} else {
			requested = new HashSet<String>(requested);
			for (Iterator<String> iterator = requested.iterator(); iterator.hasNext();) {
				String r = iterator.next();
				if (!all.contains(r)) iterator.remove();
			}
		}
		
		for (String rUri : requested) {
			try {
				Resource r = resourceRepository.findByResourceUri(rUri);
				// ask the user only for the resources associated to the user role and not managed by this client
				if (r.getAuthority().equals(AUTHORITY.ROLE_USER) && !clientAuth.getClientId().equals(r.getClientId())) {
					resources.add(r);
				}
			} catch (Exception e) {
				logger.error("Error reading resource with uri "+rUri+": "+e.getMessage());
			}
		}
		model.put("resources", resources);
		model.put("auth_request", clientAuth);
		model.put("clientName", info.getName());
		return new ModelAndView("access_confirmation", model);
	}

	/**
	 * Generate error response
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/oauth/error")
	public String handleError(Map<String,Object> model) throws Exception {
		model.put("message", "There was a problem with the OAuth2 protocol");
		return "oauth_error";
	}

	@Autowired
	public void setClientDetailsService(ClientDetailsService clientDetailsService) {
		this.clientDetailsService = clientDetailsService;
	}
}
