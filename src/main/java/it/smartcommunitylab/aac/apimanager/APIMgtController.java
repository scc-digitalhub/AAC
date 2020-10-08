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
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import it.smartcommunitylab.aac.apimanager.model.AACAPI;
import it.smartcommunitylab.aac.apimanager.model.DataList;
import it.smartcommunitylab.aac.apimanager.model.RoleModel;
import it.smartcommunitylab.aac.apimanager.model.Subscription;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.manager.ResourceManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author raman
 *
 */
@ApiIgnore
@Controller
public class APIMgtController {

	@Autowired 
	private APIManager apiManager;
	
	@Autowired
	private APIProviderManager providerManager;
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private UserManager userManager;	
	@Autowired
	private RoleManager apiRoleManager;	
	@Autowired
	private ResourceManager resourceManager;	
	
	@Value("${api.contextSpace}")
	private String apiProviderContext;

	@GetMapping("/mgmt/apis")
	public @ResponseBody DataList<AACAPI> getAPIs(
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit, 
			@RequestParam(required=false, defaultValue="") String query) throws Exception {
		return apiManager.getAPIs(offset, limit, query, getToken());
	}
	
	@GetMapping("/mgmt/apis/{apiId}")
	public @ResponseBody AACAPI getAPI(@PathVariable String apiId) throws Exception {
		
		AACAPI result = apiManager.getAPI(apiId, getToken());
		result.setApplicationRoles(resourceManager.getResourceRoles(getAPIKey(result)));
		return result;
	}
	
	private String getAPIKey(AACAPI api) {
		return api.getProvider() + "-" + api.getName() + "-"+ api.getVersion();
	} 

	@GetMapping("/mgmt/apis/{apiId}/thumbnail")
	public @ResponseBody byte[] getAPIImage(@PathVariable String apiId, HttpServletResponse res) throws Exception {
		AACAPI api = apiManager.getAPI(apiId, getToken());
		res.setContentType(MediaType.IMAGE_JPEG_VALUE);
		if (api.getThumbnailUri() != null) return apiManager.getAPIThumbnail(apiId, getToken());
		return null;
	}

	@GetMapping("/mgmt/apis/{apiId}/subscriptions")
	public @ResponseBody DataList<Subscription> getAPISubscriptions(
			@PathVariable String apiId,
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit) throws Exception 
	{
		String providerDomain = getProviderDomain();
		DataList<Subscription> subs = apiManager.getSubscriptions(apiId, providerDomain, offset, limit, getToken());
		apiRoleManager.fillRoles(subs, apiProviderContext, providerDomain);
		return subs;
	}
	

	private String getProviderDomain() throws AccessDeniedException {
		Set<Role> domainRoles = userManager.getOwnedSpaceAt(apiProviderContext);
		if (!domainRoles.isEmpty()) {
			return domainRoles.iterator().next().getSpace();
		}
		throw new AccessDeniedException("Not a APIM provider");	
	}

	@PutMapping("/mgmt/apis/userroles")
	public @ResponseBody List<String> updateRoles(@RequestBody RoleModel roleModel) throws Exception 
	{
		// cannot update PROVIDER_ROLE through this interface
		if (roleModel.getAddRoles() != null) roleModel.setAddRoles(roleModel.getAddRoles().stream().filter(r -> !Config.R_PROVIDER.equals(r)).collect(Collectors.toList()));
		if (roleModel.getRemoveRoles() != null) roleModel.setRemoveRoles(roleModel.getRemoveRoles().stream().filter(r -> !Config.R_PROVIDER.equals(r)).collect(Collectors.toList()));
		
		return apiRoleManager.updateLocalRoles(roleModel, apiProviderContext, getProviderDomain());
	} 
	

	@GetMapping("/mgmt/applications/{applicationName}/subscriptions")
	public @ResponseBody List<Subscription> getSubscriptions(@PathVariable String applicationName) throws Exception {
		String token = getToken();
		List<Subscription> subscriptions = apiManager.getSubscriptions(applicationName, token);
		return subscriptions;
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
		// find apiproviders
		List<User> users = roleManager.findUsersByRole(Config.R_PROVIDER, apiProviderContext, offset / limit, limit);
		users.forEach(u -> {
			// TODO currently assume a single owner for APIM tenant. TO BE RESOLVED
			String domain = u.contextRole(Config.R_PROVIDER, apiProviderContext).iterator().next().getSpace();
			res.add(new APIProvider(
					u.getUsername(),
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
		return result;
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
	
	@ExceptionHandler(RegistrationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public Response processRegistrationError(RegistrationException ex) {
		return Response.error(ex.getMessage());
    }
	
	@ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Response processGenericError(Exception ex) {
		return Response.error(ex.getMessage());
    }
}