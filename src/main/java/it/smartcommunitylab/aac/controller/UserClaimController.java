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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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
import it.smartcommunitylab.aac.dto.UserClaimProfileDTO;
import it.smartcommunitylab.aac.jaxbmodel.Service;
import it.smartcommunitylab.aac.manager.ClaimManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.Response;

/**
 * Controller for managing custom user claims
 * @author raman
 *
 */
//@Controller
@Api(tags = { "AAC User Claims" })
public class UserClaimController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ClaimManager claimManager;
	@Autowired
	private UserManager userManager;

	
	// TODO
	// - get single user claims for specific service
	// - update user claims for service (all at once)

	/**
	 * Read users with the claims of the specified service
	 * @param serviceId
	 * @param name optional filter
	 * @param page
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value="Get list of user with the claims of the specified service, with pagination")
	@RequestMapping(value = "/api/claims/{serviceId:.*}", method = RequestMethod.GET)
	public @ResponseBody Response getUsers(@PathVariable String serviceId, @RequestParam(required = false) String name, Pageable page) throws Exception {
		Response response = new Response();
		Page<UserClaimProfileDTO> claims = claimManager.getServiceUserClaims(userManager.getUserOrOwner(), serviceId, name, page);
		response.setData(claims);

		return response;
	}


	/**
	 * Read specific service data
	 * @return {@link Response} entity containing the service {@link Service} descriptors
	 */
	@ApiOperation(value="Get service claims for the specified User")
	@RequestMapping(value = "/api/claims/{serviceId:.*}/{userId:.*}", method = RequestMethod.GET)
	public @ResponseBody Response getClaims(@PathVariable String serviceId, @PathVariable String userId) {
		Response response = new Response();
		response.setData(claimManager.getServiceUserClaims(userManager.getUserOrOwner(), serviceId, userId));
		return response;
	} 

	/**
	 * save service data (name, id, description)
	 * @param sd
	 * @return stored {@link Service} object
	 * @throws InvalidDefinitionException 
	 */
	@ApiOperation(value="Update service claims for user")
	@RequestMapping(value="/api/claims{serviceId:.*}/{userId:.*}",method=RequestMethod.POST)
	public @ResponseBody Response saveClaims(@PathVariable String serviceId, @PathVariable String userId, @RequestBody UserClaimProfileDTO dto) throws InvalidDefinitionException {
		Response response = new Response();
		response.setData(claimManager.saveServiceUserClaims(userManager.getUserOrOwner(), serviceId, userId, dto));
		
		return response;
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
