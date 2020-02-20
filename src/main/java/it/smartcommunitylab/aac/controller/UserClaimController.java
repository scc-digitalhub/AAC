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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
import it.smartcommunitylab.aac.manager.ClaimManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.Response;

/**
 * Controller for managing custom user claims
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC Claims" })
public class UserClaimController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ClaimManager claimManager;
	@Autowired
	private UserManager userManager;

	
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
	public @ResponseBody ResponseEntity<Page<UserClaimProfileDTO>> getUsers(@PathVariable String serviceId, @RequestParam(required = false) String name, Pageable page) throws Exception {
		Page<UserClaimProfileDTO> claims = claimManager.getServiceUserClaims(userManager.getUserOrOwner(), serviceId, name, page);
		return ResponseEntity.ok(claims);
	}


	/**
	 * Read specific user claims
	 * @return {@link Response} entity containing the service {@link Service} descriptors
	 */
	@ApiOperation(value="Get service claims for the specified User")
	@RequestMapping(value = "/api/claims/{serviceId:.*}/{userId:.*}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<UserClaimProfileDTO> getClaims(@PathVariable String serviceId, @PathVariable String userId) {
		return ResponseEntity.ok(claimManager.getServiceUserClaims(userManager.getUserOrOwner(), serviceId, userId));
	} 

	@ApiOperation(value="Get service claims for the specified username")
	@RequestMapping(value = "/api/claims/{serviceId:.*}/username", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<UserClaimProfileDTO> getClaimsForUsername(@PathVariable String serviceId, @RequestParam String username) {
		return ResponseEntity.ok(claimManager.getServiceUserClaimsForUsername(userManager.getUserOrOwner(), serviceId, username));
	} 

	/**
	 * save user claims
	 * @param sd
	 * @return stored {@link Service} object
	 * @throws InvalidDefinitionException 
	 */
	@ApiOperation(value="Update service claims for user")
	@RequestMapping(value="/api/claims/{serviceId:.*}/{userId:.*}",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<UserClaimProfileDTO> saveClaims(@PathVariable String serviceId, @PathVariable String userId, @RequestBody UserClaimProfileDTO dto) throws InvalidDefinitionException {
		return ResponseEntity.ok(claimManager.saveServiceUserClaims(userManager.getUserOrOwner(), serviceId, userId, dto));
	}
	@ApiOperation(value="Update service claims for the specified username")
	@RequestMapping(value="/api/claims/{serviceId:.*}/username",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<UserClaimProfileDTO> saveClaimsForUsername(@PathVariable String serviceId, @RequestParam String username, @RequestBody UserClaimProfileDTO dto) throws InvalidDefinitionException {
		return ResponseEntity.ok(claimManager.saveServiceUserClaimsByUsername(userManager.getUserOrOwner(), serviceId, username, dto));
	}

	@ApiOperation(value="Delete service claims for user")
	@RequestMapping(value="/api/claims/{serviceId:.*}/{userId:.*}",method=RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<UserClaimProfileDTO> deletClaims(@PathVariable String serviceId, @PathVariable String userId) throws InvalidDefinitionException {
		claimManager.deleteServiceUserClaims(userManager.getUserOrOwner(), serviceId, userId);
		return ResponseEntity.ok(null);
	}
	@ApiOperation(value="Delete service claims for the specified username")
	@RequestMapping(value="/api/claims/{serviceId:.*}/username",method=RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<Void> deleteClaimsForUsername(@PathVariable String serviceId, @RequestParam String username) throws InvalidDefinitionException {
		claimManager.deleteServiceUserClaimsByUsername(userManager.getUserOrOwner(), serviceId, username);
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
