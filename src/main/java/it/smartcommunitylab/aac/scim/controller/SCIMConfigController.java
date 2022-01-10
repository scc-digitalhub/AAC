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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.FormatNotSupportedException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.SCIMResponse;
import org.wso2.charon3.core.protocol.endpoints.ResourceTypeResourceManager;
import org.wso2.charon3.core.protocol.endpoints.ServiceProviderConfigResourceManager;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.scim.service.SCIMSchemaResourceManager;
import it.smartcommunitylab.aac.scim.service.SCIMTenantAwareUserManager;

/**
 * @author raman
 *
 */
@RestController
@RequestMapping("scim/v2")
@Tag(name = "SCIM 2.0 Service Provider Configuration Endpoints" )
public class SCIMConfigController extends SCIMResourceController {

	@Autowired
	private SCIMTenantAwareUserManager tenantManager;

	@Operation(summary = "Return the SCIM specification features available on a service provider", description = "Return the SCIM specification features available on a service provider.")
	@GetMapping(value="/{realm}/ServiceProviderConfig", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> getConfig(@PathVariable String realm)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			ServiceProviderConfigResourceManager manager = new ServiceProviderConfigResourceManager();
			SCIMResponse scimResponse = manager.get(null, userManager, null, null);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}
	
	@Operation(summary = "Return information about supported resource schemas", description = "Return information about supported resource schemas.")
	@GetMapping(value="/{realm}/Schemas", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> getSchemas(@PathVariable String realm)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the store manager
			tenantManager.getUserManager(realm);
			SCIMSchemaResourceManager manager = new SCIMSchemaResourceManager();
			SCIMResponse scimResponse = manager.buildSchemaResponse();

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}

	@Operation(summary = "Return information about the specified resource schema", description = "Return information about the specified resource schema.")
	@GetMapping(value="/{realm}/Schemas/{id}", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> getSchemas(@PathVariable String realm, @PathVariable String id)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the store manager
			tenantManager.getUserManager(realm);
			SCIMSchemaResourceManager manager = new SCIMSchemaResourceManager();
			SCIMResponse scimResponse = manager.buildSchemaResponse(id);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}
	@Operation(summary = "Discover the types of resources available", description = "Discover the types of resources available.")
	@GetMapping(value="/{realm}/ResourceTypes", produces = {"application/json", "application/scim+json"})
	public @ResponseBody ResponseEntity<?> getResourceTypes(@PathVariable String realm)
			throws FormatNotSupportedException, CharonException {

		try {
			// obtain the store manager
			UserManager userManager = tenantManager.getUserManager(realm);

			ResourceTypeResourceManager manager = new ResourceTypeResourceManager();
			SCIMResponse scimResponse = manager.get(null, userManager, null, null);

			return buildResponse(realm, scimResponse);

		} catch (CharonException e) {
			throw new CharonException(e.getDetail(), e);
		}
	}
}
