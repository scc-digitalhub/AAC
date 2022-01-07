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

package it.smartcommunitylab.aac.scim.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.FormatNotSupportedException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.SCIMResponse;
import org.wso2.charon3.core.protocol.endpoints.UserResourceManager;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.api.scopes.ApiUsersScope;
import it.smartcommunitylab.aac.scim.service.SCIMProviderConstants;
import it.smartcommunitylab.aac.scim.service.SCIMTenantAwareUserManager;

/**
 * @author raman
 *
 */
@RestController
@RequestMapping("scim/v2")
@PreAuthorize("hasAuthority(this.authority)")
@Tag(name = "SCIM 2.0 /Users Endpoint" )
public class SCIMUserController extends SCIMResourceController {

	@Autowired
	private SCIMTenantAwareUserManager tenantManager;

    private final static String AUTHORITY = "SCOPE_" + ApiUsersScope.SCOPE;

	public String getAuthority() {
        return AUTHORITY;
    }

	@Operation(summary = "Return users according to the filter, sort and pagination parameters", description = "Return users according to the filter, sort and pagination parameters. Returns HTTP 404 if the users are not found.")
	@GetMapping(value="/{realm}/Users", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> getUser(
			@PathVariable String realm,
			@Parameter(description = SCIMProviderConstants.ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.ATTRIBUTES, required=false) String attribute,
			@Parameter(description = SCIMProviderConstants.EXCLUDED_ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.EXCLUDE_ATTRIBUTES, required=false) String excludedAttributes,
			@Parameter(description = SCIMProviderConstants.FILTER_DESC) @RequestParam(value=SCIMProviderConstants.FILTER) String filter,
			@Parameter(description = SCIMProviderConstants.START_INDEX_DESC) @RequestParam(value=SCIMProviderConstants.START_INDEX, defaultValue="1", required=false) int startIndex,
			@Parameter(description = SCIMProviderConstants.COUNT_DESC) @RequestParam(value=SCIMProviderConstants.COUNT, defaultValue="20", required=false) int count,
			@Parameter(description = SCIMProviderConstants.SORT_BY_DESC) @RequestParam(value=SCIMProviderConstants.SORT_BY, required=false,defaultValue="username") String sortBy,
			@Parameter(description = SCIMProviderConstants.SORT_ORDER_DESC) @RequestParam(value=SCIMProviderConstants.SORT_ORDER, required=false) String sortOrder)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the user store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM user resource manager and hand-over the request.
			UserResourceManager userResourceManager = tenantManager.getUserResourceManager(realm);

			SCIMResponse scimResponse = userResourceManager.listWithGET(userManager, filter, startIndex, count, sortBy,
					sortOrder, null, attribute, excludedAttributes);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}
	
	@Operation(summary = "Return users according to the filter, sort and pagination parameters", description = "Return users according to the filter, sort and pagination parameters. Returns HTTP 404 if the users are not found.")
	@PostMapping(value="/{realm}/Users/.search", produces = {"application/json", "application/scim+json"}, consumes= {"application/scim+json"})
	public @ResponseBody ResponseEntity<?> getUserWithPost(
			@PathVariable String realm,
			@RequestBody String requestString)
			throws FormatNotSupportedException, CharonException {

		// TODO body as object for documentation
		try {
			// obtain the user store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM user resource manager and hand-over the request.
			UserResourceManager userResourceManager = tenantManager.getUserResourceManager(realm);

			SCIMResponse scimResponse = userResourceManager.listWithPOST(requestString, userManager);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}
	
	
	@Operation(summary = "Return the user with the given id", description = "Return the user with the given id. Returns HTTP 200 if the user is found.")
	@GetMapping(value="/{realm}/Users/{id}", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> getUser(
			@PathVariable String realm,
			@Parameter(description = SCIMProviderConstants.ID_DESC) @PathVariable String id,
			@Parameter(description = SCIMProviderConstants.ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.ATTRIBUTES, required=false) String attribute,
			@Parameter(description = SCIMProviderConstants.EXCLUDED_ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.EXCLUDE_ATTRIBUTES, required=false) String excludedAttributes)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the user store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM user resource manager and hand-over the request.
			UserResourceManager userResourceManager = tenantManager.getUserResourceManager(realm);

			SCIMResponse scimResponse = userResourceManager.get(id, userManager, attribute, excludedAttributes);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}
	
//	@RequestMapping(value="/{realm}/Me", produces = {"application/json", "application/scim+json"}, consumes= {"application/scim+json"})
//	public @ResponseBody ResponseEntity<?> me() {
//		ResponseEntity.BodyBuilder builder = ResponseEntity.status(ResponseCodeConstants.CODE_NOT_IMPLEMENTED);
//        builder.header(SCIMConstants.CONTENT_TYPE_HEADER, SCIMConstants.APPLICATION_JSON);
//        return builder.body(ResponseCodeConstants.DESC_NOT_IMPLEMENTED);
//		
//	}


}
