/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Multimap;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.manager.RoleManager;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ClientDetailsService clientDetailsService;
	@Autowired
	private ResourceRepository resourceRepository;
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private ApprovalStore approvalStore;

	/**
	 * Request the user confirmation for the resources enabled for the requesting client
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/oauth/confirm_access")
	public ModelAndView getAccessConfirmation(Map<String, Object> model, Principal principal) throws Exception {
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
		User user = (User) ((Authentication) principal).getPrincipal();
		Collection<Approval> approvals = approvalStore.getApprovals(user.getUsername(), clientAuth.getClientId());
		Date now = new Date();
		Set<String> approved = approvals != null 
				? approvals.stream()
						.filter(a -> a.isApproved() && a.getExpiresAt().after(now))
						.map(a -> a.getScope())
						.collect(Collectors.toSet()) 
				: Collections.emptySet();
		
		for (String rUri : requested) {
			if (approved.contains(rUri)) continue;
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
		
		// if trusted client no need to ask
		if (clientAuth.getAuthorities() != null) {
			for (GrantedAuthority ga : clientAuth.getAuthorities()) {
				if (Config.AUTHORITY.ROLE_CLIENT_TRUSTED.toString().equals(ga.getAuthority())) {
					resources.clear();
				}
			}
		}
		
		Multimap<String, String> spaces = roleManager.getRoleSpacesToNarrow(clientAuth.getClientId(), ((Authentication) principal).getAuthorities());

		if (resources.size() == 0 && (spaces == null || spaces.isEmpty())) {
			clientAuth.setApproved(true);
			model.put("auth_request", clientAuth);
			return new ModelAndView("redirect:./authorize");
		} 
		
		model.put("resources", resources);
		model.put("auth_request", clientAuth);
		model.put("clientName", info.getName());
		model.put("spaces", spaces == null ? Collections.emptyMap() : spaces);
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
