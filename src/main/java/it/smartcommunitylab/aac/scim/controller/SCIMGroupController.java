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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.FormatNotSupportedException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.SCIMResponse;
import org.wso2.charon3.core.protocol.endpoints.GroupResourceManager;

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
@Tag(name = "SCIM 2.0 /Groups Endpoint" )
public class SCIMGroupController extends SCIMResourceController {

	@Autowired
	private SCIMTenantAwareUserManager tenantManager;

    private final static String AUTHORITY = "SCOPE_" + ApiUsersScope.SCOPE;

	public String getAuthority() {
        return AUTHORITY;
    }

	@Operation(summary = "Return groups according to the filter, sort and pagination parameters", description = "Return groups according to the filter, sort and pagination parameters. Returns HTTP 404 if the groups are not found.")
	@GetMapping(value="/{realm}/Groups", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> getGroups(
			@PathVariable String realm,
			@Parameter(description = SCIMProviderConstants.ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.ATTRIBUTES, required=false) String attribute,
			@Parameter(description = SCIMProviderConstants.EXCLUDED_ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.EXCLUDE_ATTRIBUTES, required=false) String excludedAttributes,
			@Parameter(description = SCIMProviderConstants.FILTER_DESC) @RequestParam(value=SCIMProviderConstants.FILTER, required = false) String filter,
			@Parameter(description = SCIMProviderConstants.START_INDEX_DESC) @RequestParam(value=SCIMProviderConstants.START_INDEX, defaultValue="1", required=false) int startIndex,
			@Parameter(description = SCIMProviderConstants.COUNT_DESC) @RequestParam(value=SCIMProviderConstants.COUNT, defaultValue="20", required=false) int count,
			@Parameter(description = SCIMProviderConstants.SORT_BY_DESC) @RequestParam(value=SCIMProviderConstants.SORT_BY, required=false,defaultValue="displayName") String sortBy,
			@Parameter(description = SCIMProviderConstants.SORT_ORDER_DESC) @RequestParam(value=SCIMProviderConstants.SORT_ORDER, required=false) String sortOrder)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the group store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM group resource manager and hand-over the request.
			GroupResourceManager groupResourceManager = tenantManager.getGroupResourceManager(realm);

			SCIMResponse scimResponse = groupResourceManager.listWithGET(userManager, filter, startIndex, count, sortBy,
					sortOrder, null, attribute, excludedAttributes);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}
	
	@Operation(summary = "Return groups according to the filter, sort and pagination parameters", description = "Return groups according to the filter, sort and pagination parameters. Returns HTTP 404 if the groups are not found.")
	@PostMapping(value="/{realm}/Groups/.search", produces = {"application/json", "application/scim+json"}, consumes= {"application/scim+json"})
	public @ResponseBody ResponseEntity<?> getGroupsWithPost(
			@PathVariable String realm,
			@RequestBody String requestString)
			throws FormatNotSupportedException, CharonException {

		// TODO body as object for documentation
		try {
			// obtain the group store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM group resource manager and hand-over the request.
			GroupResourceManager groupResourceManager = tenantManager.getGroupResourceManager(realm);

			SCIMResponse scimResponse = groupResourceManager.listWithPOST(requestString, userManager);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}
	
	
	@Operation(summary = "Return the group with the given id", description = "Return the group with the given id. Returns HTTP 200 if the group is found.")
	@GetMapping(value="/{realm}/Groups/{id}", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> getGroup(
			@PathVariable String realm,
			@Parameter(description = SCIMProviderConstants.ID_DESC) @PathVariable String id,
			@Parameter(description = SCIMProviderConstants.ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.ATTRIBUTES, required=false) String attribute,
			@Parameter(description = SCIMProviderConstants.EXCLUDED_ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.EXCLUDE_ATTRIBUTES, required=false) String excludedAttributes)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the group store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM group resource manager and hand-over the request.
			GroupResourceManager groupResourceManager = tenantManager.getGroupResourceManager(realm);

			SCIMResponse scimResponse = groupResourceManager.get(id, userManager, attribute, excludedAttributes);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}

	
	@Operation(summary = "Create and return the group which was created", description = "Return the group which was created. Returns HTTP 201 if the group is successfully created.")
	@PostMapping(value="/{realm}/Groups", produces = {"application/json", "application/scim+json"}, consumes= {"application/scim+json"})
	public @ResponseBody ResponseEntity<?> createGroup(
			@PathVariable String realm,
			@RequestBody String requestString,
			@Parameter(description = SCIMProviderConstants.ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.ATTRIBUTES, required=false) String attribute,
			@Parameter(description = SCIMProviderConstants.EXCLUDED_ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.EXCLUDE_ATTRIBUTES, required=false) String excludedAttributes) throws CharonException {
		
		try {
			// obtain the group store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM group resource manager and hand-over the request.
			GroupResourceManager groupResourceManager = tenantManager.getGroupResourceManager(realm);

			SCIMResponse scimResponse = groupResourceManager.create(requestString, userManager, attribute, excludedAttributes);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}


	@Operation(summary = "Update and return the updated group", description = "Return the updated group. Returns HTTP 404 if the group is not found.")
	@PutMapping(value="/{realm}/Groups/{id}", produces = {"application/json", "application/scim+json"}, consumes= {"application/scim+json"})
	public @ResponseBody ResponseEntity<?> updateGroup(
			@PathVariable String realm,
			@Parameter(description = SCIMProviderConstants.ID_DESC) @PathVariable String id,
			@RequestBody String requestString,
			@Parameter(description = SCIMProviderConstants.ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.ATTRIBUTES, required=false) String attribute,
			@Parameter(description = SCIMProviderConstants.EXCLUDED_ATTRIBUTES_DESC) @RequestParam(value=SCIMProviderConstants.EXCLUDE_ATTRIBUTES, required=false) String excludedAttributes) throws CharonException {
		
		try {
			// obtain the group store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM group resource manager and hand-over the request.
			GroupResourceManager groupResourceManager = tenantManager.getGroupResourceManager(realm);

			SCIMResponse scimResponse = groupResourceManager.updateWithPUT(id, requestString, userManager, attribute, excludedAttributes);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}

	@Operation(summary = "Delete the group with the given id", description = "Delete the group with the given id. Returns HTTP 204 if the group is successfully deleted.")
	@DeleteMapping(value="/{realm}/Groups/{id}", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> deleteUser(
			@PathVariable String realm,
			@Parameter(description = SCIMProviderConstants.ID_DESC) @PathVariable String id)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the group store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			// create charon-SCIM group resource manager and hand-over the request.
			GroupResourceManager groupResourceManager = tenantManager.getGroupResourceManager(realm);

			SCIMResponse scimResponse = groupResourceManager.delete(id, userManager);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}	

}
