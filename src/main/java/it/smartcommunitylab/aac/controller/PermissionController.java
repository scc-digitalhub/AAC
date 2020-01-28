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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceClaimDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;
import it.smartcommunitylab.aac.jaxbmodel.Service;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Permissions;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for managing the permissions of the apps
 * @author raman
 *
 */
@ApiIgnore
@Controller
public class PermissionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final Integer RA_NONE = 0;
	private static final Integer RA_APPROVED = 1;
	private static final Integer RA_REJECTED = 2;
	private static final Integer RA_PENDING = 3;

	@Autowired
	private ServiceManager serviceManager;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private UserManager userManager;
	
	/**
	 * Save permissions requested by the app.
	 * @param permissions
	 * @param clientId
	 * @param serviceId
	 * @return {@link Response} entity containing the processed app {@link Permissions} descriptor
	 * @throws Exception 
	 */
	@RequestMapping(value="/dev/permissions/{clientId}/{serviceId:.*}",method=RequestMethod.PUT)
	public @ResponseBody Response savePermissions(@RequestBody Permissions permissions, @PathVariable String clientId, @PathVariable String serviceId) throws Exception {
		Response response = new Response();
		// check that the client is owned by the current user
		userManager.checkClientIdOwnership(clientId);
		ClientDetailsEntity clientDetails = clientDetailsRepository.findByClientId(clientId);
		ClientAppInfo info = ClientAppInfo.convert(clientDetails.getAdditionalInformation());
		
		if (info.getScopeApprovals() == null) info.setScopeApprovals(new HashMap<String, Boolean>());
		Set<String> scopes = new HashSet<String>(clientDetails.getScope());
		for (String r : permissions.getSelectedScopes().keySet()) {
			ServiceScopeDTO scopeObj = serviceManager.getServiceScopeDTO(r);
			// if not checked, remove from permissions and from pending requests
			if (!permissions.getSelectedScopes().get(r)) {
				info.getScopeApprovals().remove(r);
				scopes.remove(scopeObj.getScope());
			// if checked but requires approval, check whether
		    // - already approved (i.e., included in client scopes)
			// - already requested (i.e., included in additional info approval requests map)	
			} else if (scopeObj.isApprovalRequired()) {
				if (!scopes.contains(r) && !info.getScopeApprovals().containsKey(r)) {
					info.getScopeApprovals().put(r, true);
				}
			// if approval is not required, include directly in client resource ids	
			} else {
				scopes.add(scopeObj.getScope());
			}
		}
		clientDetails.setResourceIds(StringUtils.collectionToCommaDelimitedString(serviceManager.findServiceIdsByScopes(scopes)));
		clientDetails.setScope(StringUtils.collectionToCommaDelimitedString(scopes));
		clientDetails.setAdditionalInformation(info.toJson());
		clientDetailsRepository.save(clientDetails);
		response.setData(buildPermissions(clientDetails, serviceId));
		
		return response;
		
	}
	

	/**
	 * Read permissions of the specified app and service
	 * @param clientId
	 * @param serviceId
	 * @return {@link Response} entity containing the app {@link Permissions} descriptor
	 */
	@RequestMapping(value="/dev/permissions/{clientId}/{serviceId:.*}",method=RequestMethod.GET)
	public @ResponseBody Response getPermissions(@PathVariable String clientId, @PathVariable String serviceId) {
		Response response = new Response();
		userManager.checkClientIdOwnership(clientId);
		ClientDetailsEntity clientDetails = clientDetailsRepository.findByClientId(clientId);
		Permissions permissions = buildPermissions(clientDetails, serviceId);
		response.setData(permissions);
		
		return response;
	}

	
	/**
	 * Read services
	 * @param clientId
	 * @return {@link Response} entity containing the service {@link Service} descriptors
	 */
	@RequestMapping(value = "/dev/services/{clientId}", method = RequestMethod.GET)
	public @ResponseBody Response getServices(@PathVariable String clientId, @RequestParam(required = false) String name, Pageable page) throws Exception {
		Response response = new Response();
		userManager.checkClientIdOwnership(clientId);

		Page<ServiceDTO> services = serviceManager.getAllServices(name, page);
		response.setData(services);

		return response;
	}


	/**
	 * Read services defined by the current user
	 * @return {@link Response} entity containing the service {@link Service} descriptors
	 */
	@RequestMapping(value="/dev/services/my",method=RequestMethod.GET)
	public @ResponseBody Response myServices() {
		Response response = new Response();
		response.setData(serviceManager.getUserServices(userManager.getUser()));
		return response;
	} 

	/**
	 * Read service contexts available to the current user
	 * @return {@link Response} entity containing the service contexts
	 */
	@RequestMapping(value="/dev/servicecontexts/my",method=RequestMethod.GET)
	public @ResponseBody Response myContexts() {
		Response response = new Response();
		response.setData(serviceManager.getUserContexts(userManager.getUser()));
		return response;
	} 

	/**
	 * Read services defined by the current user
	 * @return {@link Response} entity containing the service {@link Service} descriptors
	 */
	@RequestMapping(value="/dev/services/my/{serviceId:.*}",method=RequestMethod.GET)
	public @ResponseBody Response myService(@PathVariable String serviceId) {
		Response response = new Response();
		ServiceDTO service = serviceManager.getService(serviceId);
		service.setScopes(serviceManager.getServiceScopes(serviceId));
		service.setClaims(serviceManager.getServiceClaims(serviceId));
		response.setData(service);
		return response;
	} 

	/**
	 * save service data (name, id, description)
	 * @param sd
	 * @return stored {@link Service} object
	 */
	@RequestMapping(value="/dev/services/my",method=RequestMethod.POST)
	public @ResponseBody Response saveService(@RequestBody ServiceDTO sd) {
		Response response = new Response();
		response.setData(serviceManager.saveService(userManager.getUser(), sd));
		
		return response;
	}

	/**
	 * Delete service object if possible
	 * @param serviceId
	 * @return
	 */
	@RequestMapping(value="/dev/services/my/{serviceId:.*}",method=RequestMethod.DELETE)
	public @ResponseBody Response deleteService(@PathVariable String serviceId) {
		Response response = new Response();
		serviceManager.deleteService(userManager.getUser(), serviceId);
		
		return response;
	}
	
	/**
	 * Validate claim mapping
	 * @return {@link Response} entity containing the claims for the current user or validation error ingfo
	 * @throws InvalidDefinitionException 
	 */
	@RequestMapping(value="/dev/services/my/{serviceId}/claimmapping/validate", method=RequestMethod.POST)
	public @ResponseBody Response validateClaimMapping(@RequestBody ServiceDTO sd, @PathVariable String serviceId, @RequestParam(required = false) Set<String> scopes) throws InvalidDefinitionException {
		Response response = new Response();
		User user = userManager.getUser();
		response.setData(serviceManager.validateClaimMapping(user, sd, scopes == null ? Collections.emptySet() : scopes));
		return response;
	}
	/**
	 * Add / edit scope for the service object if possible
	 * @param serviceId
	 * @return
	 */
	@RequestMapping(value="/dev/services/my/{serviceId}/scope",method=RequestMethod.PUT)
	public @ResponseBody Response addScope(@PathVariable String serviceId, @RequestBody ServiceScopeDTO scope) {
		Response response = new Response();
		response.setData(serviceManager.saveServiceScope(userManager.getUser(), serviceId, scope));
		return response;
	}

	/**
	 * Delete scope declaration from the service object if possible
	 * @param serviceId
	 * @param id 
	 * @return
	 */
	@RequestMapping(value="/dev/services/my/{serviceId}/scope/{scope:.*}",method=RequestMethod.DELETE)
	public @ResponseBody Response deleteScope(@PathVariable String serviceId, @PathVariable String scope) {
		Response response = new Response();
		serviceManager.deleteServiceScope(userManager.getUser(), serviceId, scope);
		return response;
	}

	/**
	 * Add / edit scope for the service object if possible
	 * @param serviceId
	 * @return
	 */
	@RequestMapping(value="/dev/services/my/{serviceId}/claim",method=RequestMethod.PUT)
	public @ResponseBody Response addClaim(@PathVariable String serviceId, @RequestBody ServiceClaimDTO scope) {
		Response response = new Response();
		response.setData(serviceManager.saveServiceClaim(userManager.getUser(), serviceId, scope));
		return response;
	}

	/**
	 * Delete scope declaration from the service object if possible
	 * @param serviceId
	 * @param id 
	 * @return
	 */
	@RequestMapping(value="/dev/services/my/{serviceId}/claim/{claim:.*}",method=RequestMethod.DELETE)
	public @ResponseBody Response deleteClaim(@PathVariable String serviceId, @PathVariable String claim) {
		Response response = new Response();
		serviceManager.deleteServiceClaim(userManager.getUser(), serviceId, claim);
		return response;
	}

	/**
	 * Create {@link Permissions} descriptors from the client app data.
	 * @param clientId
	 * @param clientDetails
	 * @param serviceId 
	 * @return
	 */
	protected Permissions buildPermissions(ClientDetailsEntity clientDetails, String serviceId) {
		Permissions permissions = new Permissions();
		permissions.setService(serviceManager.getService(serviceId));
		// map scopes selected by the client
		Map<String,Boolean> selectedScopes = new HashMap<String, Boolean>();
		// map scopes approval state
		Map<String,Integer> scopeApprovals = new HashMap<String, Integer>();
		Set<String> set = clientDetails.getScope();
		if (set == null) set = Collections.emptySet();
		
		List<ServiceScopeDTO> allScopes = serviceManager.getServiceScopes(serviceId);
		// read approval status for the resources that require approval explicitly
		ClientAppInfo info = ClientAppInfo.convert(clientDetails.getAdditionalInformation());
		if (info.getScopeApprovals() == null) info.setScopeApprovals(Collections.<String,Boolean>emptyMap());
		
		if (allScopes != null) {
			for (ServiceScopeDTO resource : allScopes) {
				String rId = resource.getScope();
				selectedScopes.put(rId, set.contains(rId) || info.getScopeApprovals().containsKey(rId));
				// set the resource approval status
				if (selectedScopes.getOrDefault(rId, false)) {
					// the resource is approved if it is in the client scopes
					if (set.contains(rId)) scopeApprovals.put(rId, RA_APPROVED);
					// if resource is not in the resource approvals map, then it is rejected
					else if (!info.getScopeApprovals().get(rId)) scopeApprovals.put(rId, RA_REJECTED);
					// resource is waiting for approval
					else scopeApprovals.put(rId, RA_PENDING);
				} else {
					scopeApprovals.put(rId, RA_NONE);
				}
			}
		}
		
		permissions.setScopeApprovals(scopeApprovals);
		permissions.setSelectedScopes(selectedScopes);
		permissions.setAvailableScopes(allScopes);
		return permissions;
	}

	@ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Response processAccessError(AccessDeniedException ex) {
		return Response.error(ex.getMessage());
    }

	@ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processValidationError(MethodArgumentNotValidException ex) {
        BindingResult br = ex.getBindingResult();
        List<FieldError> fieldErrors = br.getFieldErrors();
        StringBuilder builder = new StringBuilder();
        
        fieldErrors.forEach(fe -> builder.append(fe.getDefaultMessage()).append("\n"));
        
		return Response.error(builder.toString());
    }

	@ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processDataError(IllegalArgumentException ex) {
		return Response.error(ex.getMessage());
    }

	@ExceptionHandler(InvalidDefinitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processDefinitionError(InvalidDefinitionException ex) {
		return Response.error(ex.getMessage());
    }
	
	@ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Response processGenericError(Exception ex) {
		logger.error(ex.getMessage(), ex);
		return Response.error(ex.getMessage());
    }

}
