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

package it.smartcommunitylab.aac.apimanager;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.apimanager.model.AACAPI;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.manager.ResourceManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Response.RESPONSE;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.wso2.model.API;
import it.smartcommunitylab.aac.wso2.model.APIInfo;
import it.smartcommunitylab.aac.wso2.model.DataList;
import it.smartcommunitylab.aac.wso2.model.RoleModel;
import it.smartcommunitylab.aac.wso2.model.Subscription;
import it.smartcommunitylab.aac.wso2.services.APIPublisherService;
import it.smartcommunitylab.aac.wso2.services.UserManagementService;

/**
 * @author raman
 *
 */
@Controller
public class APIMgtController {

	@Autowired
	private APIPublisherService pub;
	@Autowired
	private UserManagementService umService;
	@Autowired
	private APIProviderManager providerManager;
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private UserManager userManager;	
	@Autowired
	private APIRoleManager apiRoleManager;	
	@Autowired
	private ResourceManager resourceManager;	
	
	@GetMapping("/mgmt/apis")
	public @ResponseBody DataList<APIInfo> getAPIs(
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit, 
			@RequestParam(required=false, defaultValue="") String query) throws Exception {
		return pub.getAPIs(offset, limit, query, getToken());
	}
	
	@GetMapping("/mgmt/apis/{apiId}")
	public @ResponseBody AACAPI getAPI(@PathVariable String apiId) throws Exception {
		API api = pub.getAPI(apiId, getToken());
		
		if (api.getThumbnailUri() != null) {
			api.setThumbnailUri("/mgmt"+api.getThumbnailUri());
		}
		AACAPI result = new AACAPI(api);
		result.setApplicationRoles(resourceManager.getResourceRoles(Utils.getAPIKey(api)));
		return result;
	}
	
	@GetMapping("/mgmt/apis/{apiId}/thumbnail")
	public @ResponseBody byte[] getAPI(@PathVariable String apiId, HttpServletResponse res) throws Exception {
		API api = pub.getAPI(apiId, getToken());
		res.setContentType(MediaType.IMAGE_JPEG_VALUE);
		if (api.getThumbnailUri() != null) return pub.getAPIThumbnail(apiId, getToken());
		return null;
	}

	@GetMapping("/mgmt/apis/{apiId}/subscriptions")
	public @ResponseBody DataList<Subscription> getAPISubscriptions(
			@PathVariable String apiId,
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit) throws Exception 
	{
		DataList<Subscription> subs = pub.getSubscriptions(apiId, userManager.getProviderDomain(), offset, limit, getToken());
		apiRoleManager.fillRoles(subs);
		return subs;
	}
	


	@PutMapping("/mgmt/apis/{apiId}/userroles")
	public @ResponseBody List<String> updateRoles(@PathVariable String apiId, @RequestBody RoleModel roleModel) throws Exception 
	{
//		umService.updateRoles(roleModel, roleModel.getUser(), userManager.getProviderDomain());
//		List<String> roles = pub.getUserAPIRoles(apiId, roleModel.getUser(), userManager.getProviderDomain(), getToken());
		return apiRoleManager.updateLocalRoles(roleModel);
	} 
	

	
	
	/**
	 * @return 
	 * @throws Exception 
	 */
	private String getToken() throws Exception {
		try {
			return providerManager.createToken();
		} catch (Exception e) {
			throw new AccessDeniedException(e.getMessage());
		}
	}

	@GetMapping("/admin/apiproviders")
	public @ResponseBody DataList<APIProvider> providers(
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit) 
	{
		List<APIProvider> res = new LinkedList<>();
		List<User> users = roleManager.findUsersByRole(ROLE_SCOPE.tenant, UserManager.R_PROVIDER, offset / limit, limit);
		users.forEach(u -> {
			String domain = u.role(ROLE_SCOPE.tenant, UserManager.R_PROVIDER).iterator().next().getContext();
			res.add(new APIProvider(
					u.attributeValue(Config.IDP_INTERNAL, "email"),
					u.getName(),
					u.getSurname(),
					domain,
					u.attributeValue(Config.IDP_INTERNAL, "lang")
					));
		});
		DataList<APIProvider> dataList = new DataList<>();
		dataList.setList(res);
		return dataList;
	}
	
	@PostMapping("/admin/apiproviders")
	public @ResponseBody Response createAPIProvider(@Valid @RequestBody APIProvider provider) {
		
		providerManager.createAPIProvider(provider);
		Response result = new Response();
		result.setResponseCode(RESPONSE.OK);
		return result;
	}
	

	@ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Response processAccessError(AccessDeniedException ex) {
		Response result = new Response();
		result.setResponseCode(RESPONSE.ERROR);
		result.setErrorMessage(ex.getMessage());
		return result;
    }

	@ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processValidationError(MethodArgumentNotValidException ex) {
        BindingResult br = ex.getBindingResult();
        List<FieldError> fieldErrors = br.getFieldErrors();
        StringBuilder builder = new StringBuilder();
        
        fieldErrors.forEach(fe -> builder.append(fe.getDefaultMessage()).append("\n"));
        
		Response result = new Response();
		result.setResponseCode(RESPONSE.ERROR);
		result.setErrorMessage(builder.toString());
		return result;
    }
	
	@ExceptionHandler(RegistrationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public Response processRegistrationError(RegistrationException ex) {
		Response result = new Response();
		result.setResponseCode(RESPONSE.ERROR);
		result.setErrorMessage(ex.getMessage());
		return result;

    }
}
