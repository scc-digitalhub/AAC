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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.aac.jaxbmodel.ResourceMapping;
import it.smartcommunitylab.aac.jaxbmodel.Service;
import it.smartcommunitylab.aac.manager.ResourceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Permissions;
import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.ResourceRepository;
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
	private ResourceManager resourceManager;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private ResourceRepository resourceRepository;
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
		
		if (info.getResourceApprovals() == null) info.setResourceApprovals(new HashMap<String, Boolean>());
		Collection<String> resourceIds = new HashSet<String>(clientDetails.getResourceIds());
		Collection<String> scopes = new HashSet<String>(clientDetails.getScope());
		for (String r : permissions.getSelectedResources().keySet()) {
			Resource resource = resourceRepository.findOne(Long.parseLong(r));
			// if not checked, remove from permissions and from pending requests
			if (!permissions.getSelectedResources().get(r)) {
				info.getResourceApprovals().remove(r);
				resourceIds.remove(r);
				scopes.remove(resource.getResourceUri());
			// if checked but requires approval, check whether
			// - is the resource of the same client, so add automatically	
		    // - already approved (i.e., included in client resourceIds)
			// - already requested (i.e., included in additional info approval requests map)	
			} else if (!clientId.equals(resource.getClientId()) && resource.isApprovalRequired()) {
				if (!resourceIds.contains(r) && ! info.getResourceApprovals().containsKey(r)) {
					info.getResourceApprovals().put(r, true);
				}
			// if approval is not required, include directly in client resource ids	
			} else {
				resourceIds.add(r);
				scopes.add(resource.getResourceUri());
			}
		}
		clientDetails.setResourceIds(StringUtils.collectionToCommaDelimitedString(resourceIds));
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
	public @ResponseBody Response getServices(@PathVariable String clientId) throws Exception {
		Response response = new Response();
		userManager.checkClientIdOwnership(clientId);

		List<Service> services = resourceManager.getServiceObjects();
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
		response.setData(resourceManager.getServiceObjects(""+userManager.getUserId()));
		
		return response;
	} 

	/**
	 * save service data (name, id, description)
	 * @param sd
	 * @return stored {@link Service} object
	 */
	@RequestMapping(value="/dev/services/my",method=RequestMethod.POST)
	public @ResponseBody Response saveService(@RequestBody Service sd) {
		Response response = new Response();
		response.setData(resourceManager.saveServiceObject(sd, userManager.getUserId()));
		
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
		resourceManager.checkServiceOwnership(serviceId,userManager.getUserId().toString());
		resourceManager.deleteService(serviceId, userManager.getUserId().toString());
		
		return response;
	}
	
	/**
	 * Add resource parameter to the service object if possible
	 * @param serviceId
	 * @return
	 */
	@RequestMapping(value="/dev/services/my/{serviceId}/mapping",method=RequestMethod.PUT)
	public @ResponseBody Response addMapping(@PathVariable String serviceId, @RequestBody ResourceMapping mapping) {
		Response response = new Response();
		resourceManager.checkServiceOwnership(serviceId,userManager.getUserId().toString());
		response.setData(resourceManager.addMapping(serviceId, mapping, userManager.getUserId().toString()));
		return response;
	}

	/**
	 * Delete resource parameter declaration from the service object if possible
	 * @param serviceId
	 * @param id 
	 * @return
	 */
	@RequestMapping(value="/dev/services/my/{serviceId}/mapping/{id:.*}",method=RequestMethod.DELETE)
	public @ResponseBody Response deleteMapping(@PathVariable String serviceId, @PathVariable String id) {
		Response response = new Response();
		resourceManager.checkServiceOwnership(serviceId,userManager.getUserId().toString());
		response.setData(resourceManager.removeMapping(serviceId, id, userManager.getUserId().toString()));
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
		permissions.setService(resourceManager.getServiceObject(serviceId));
		// read all available resources and assign permission status
		Map<String, List<Resource>> otherResourcesMap = new HashMap<String, List<Resource>>();
		// map resources selected by the client
		Map<String,Boolean> selectedResources = new HashMap<String, Boolean>();
		// map resource approval state
		Map<String,Integer> resourceApprovals = new HashMap<String, Integer>();
		Set<String> set = clientDetails.getResourceIds();
		if (set == null) set = Collections.emptySet();
		
		List<Resource> otherResources = resourceManager.getAvailableResources(clientDetails.getClientId(), userManager.getUserId());
		// read approval status for the resources that require approval explicitly
		ClientAppInfo info = ClientAppInfo.convert(clientDetails.getAdditionalInformation());
		if (info.getResourceApprovals() == null) info.setResourceApprovals(Collections.<String,Boolean>emptyMap());
		
		if (otherResources != null) {
			for (Resource resource : otherResources) {
				String rId = resource.getResourceId().toString();
				List<Resource> sublist = otherResourcesMap.get(resource.getResourceType());
				if (sublist == null) {
					sublist = new ArrayList<Resource>();
					otherResourcesMap.put(resource.getResourceType(), sublist);
				}
				sublist.add(resource);
				selectedResources.put(rId, set.contains(rId) || info.getResourceApprovals().containsKey(rId));
				// set the resource approval status
				if (selectedResources.containsKey(rId) && selectedResources.get(rId)) {
					// the resource is approved if it is in the client resource Ids
					if (set.contains(rId)) resourceApprovals.put(rId, RA_APPROVED);
					// if resource is not in the resource approvals map, then it is rejected
					else if (!info.getResourceApprovals().get(rId)) resourceApprovals.put(rId, RA_REJECTED);
					// resource is waiting for approval
					else resourceApprovals.put(rId, RA_PENDING);
				} else {
					resourceApprovals.put(rId, RA_NONE);
				}
			}
		}
		Map<String,List<Resource>> serviceMap = new TreeMap<String, List<Resource>>();
		serviceMap.put("__", new ArrayList<Resource>());
		for (ResourceMapping rm : permissions.getService().getResourceMapping()) {
			List<Resource> list = otherResourcesMap.get(rm.getId());
			if (list != null) {
				for (Resource r : list) {
					String key = "__";
					List<Resource> targetList = serviceMap.get(key);
					if (targetList == null) {
						targetList = new ArrayList<Resource>();
						serviceMap.put(key, targetList);
					}
					targetList.add(r);
				}
			}
		}
		
		permissions.setResourceApprovals(resourceApprovals);
		permissions.setSelectedResources(selectedResources);
		permissions.setAvailableResources(serviceMap);
		return permissions;
	}

	@ExceptionHandler(AccessDeniedException.class)
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
	
	
	@ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Response processGenericError(Exception ex) {
		logger.error(ex.getMessage(), ex);
		return Response.error(ex.getMessage());
    }

}
