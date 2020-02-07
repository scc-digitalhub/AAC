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

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceClaimDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;
import it.smartcommunitylab.aac.jaxbmodel.Service;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.Response;

/**
 * Controller for managing the services (resources), scopes, and claims
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC Services" })
public class ServiceController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ServiceManager serviceManager;
	@Autowired
	private UserManager userManager;
	
	/**
	 * Read services
	 * @param name text search filter to apply to service name
	 * @param page paging configuration
	 * @return {@link Response} entity containing the service {@link Service} descriptors
	 */
	@ApiOperation(value="Get list of all services, with pagination")
	@RequestMapping(value = "/api/services", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<Page<ServiceDTO>> getServices(@RequestParam(required = false) String name, Pageable page) throws Exception {
		return ResponseEntity.ok(serviceManager.getAllServices(name, page));
	}

	/**
	 * Read services managed by current user
	 * @param name text search filter to apply to service name
	 * @param page paging configuration
	 * @return {@link Response} entity containing the service {@link Service} descriptors
	 */
	@ApiOperation(value="Get list of user services")
	@RequestMapping(value = "/api/services/me", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<List<ServiceDTO>> getMyServices() throws Exception {
		// exclude core services (with empty namespace)
		return ResponseEntity.ok(serviceManager.getUserServices(userManager.getUserOrOwner()).stream().filter(s -> !StringUtils.isEmpty(s.getNamespace())).collect(Collectors.toList()));
	}

	/**
	 * Read specific service data
	 * @return {@link Response} entity containing the service {@link Service} descriptors
	 */
	@ApiOperation(value="Get a service definition")
	@RequestMapping(value="/api/services/{serviceId:.*}",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity<ServiceDTO> getService(@PathVariable String serviceId) {
		ServiceDTO service = serviceManager.getService(serviceId);
		if (service != null) {
			service.setScopes(serviceManager.getServiceScopes(serviceId));
			service.setClaims(serviceManager.getServiceClaims(serviceId));
		}
		return ResponseEntity.ok(service);
	} 

	/**
	 * save service data (name, id, description)
	 * @param sd
	 * @return stored {@link Service} object
	 */
	@ApiOperation(value="Create or update a service definition")
	@RequestMapping(value="/api/services",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<ServiceDTO> saveService(@RequestBody ServiceDTO sd) {
		return ResponseEntity.ok(serviceManager.saveService(userManager.getUserOrOwner(), sd));
	}

	/**
	 * Delete service object if possible
	 * @param serviceId
	 * @return
	 */
	@ApiOperation(value="Delete a service definition")
	@RequestMapping(value="/api/services/{serviceId:.*}",method=RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<Void> deleteService(@PathVariable String serviceId) {
		serviceManager.deleteService(userManager.getUserOrOwner(), serviceId);
		return ResponseEntity.ok(null);
	}

	/**
	 * Add / edit scope for the service object if possible
	 * @param serviceId
	 * @return
	 */
	@ApiOperation(value="Create or update scope definition")
	@RequestMapping(value="/api/services/{serviceId}/scope",method=RequestMethod.PUT)
	public @ResponseBody ResponseEntity<ServiceScopeDTO> addScope(@PathVariable String serviceId, @RequestBody ServiceScopeDTO scope) {
		return ResponseEntity.ok(serviceManager.saveServiceScope(userManager.getUserOrOwner(), serviceId, scope));
	}

	/**
	 * Delete scope declaration from the service object if possible
	 * @param serviceId
	 * @param id 
	 * @return
	 */
	@ApiOperation(value="Delete a scope definition")
	@RequestMapping(value="/api/services/{serviceId}/scope/{scope:.*}",method=RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<Void> deleteScope(@PathVariable String serviceId, @PathVariable String scope) {
		serviceManager.deleteServiceScope(userManager.getUserOrOwner(), serviceId, scope);
		return ResponseEntity.ok(null);
	}

	/**
	 * Add / edit scope for the service object if possible
	 * @param serviceId
	 * @return
	 */
	@ApiOperation(value="Create or update claim definition")
	@RequestMapping(value="/api/services/{serviceId}/claim",method=RequestMethod.PUT)
	public @ResponseBody ResponseEntity<ServiceClaimDTO> addClaim(@PathVariable String serviceId, @RequestBody ServiceClaimDTO claim) {
		return ResponseEntity.ok(serviceManager.saveServiceClaim(userManager.getUserOrOwner(), serviceId, claim));
	}

	/**
	 * Delete scope declaration from the service object if possible
	 * @param serviceId
	 * @param id 
	 * @return
	 */
	@ApiOperation(value="Delete claim definition")
	@RequestMapping(value="/api/services/{serviceId}/claim/{claim:.*}",method=RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<Void> deleteClaim(@PathVariable String serviceId, @PathVariable String claim) {
		serviceManager.deleteServiceClaim(userManager.getUserOrOwner(), serviceId, claim);
		return ResponseEntity.ok(null);
	}
	
	
	@ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Response processAccessError(SecurityException ex) {
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
