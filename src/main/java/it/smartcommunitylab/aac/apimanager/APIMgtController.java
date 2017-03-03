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

import java.rmi.RemoteException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.AxisFault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;

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
	private UserManagementService isService;
	@Autowired
	private APIProviderManager tokenEmitter;
	
	
	@GetMapping("/mgmt/apis")
	public @ResponseBody DataList<APIInfo> getAPIs(
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit, 
			@RequestParam(required=false, defaultValue="") String query) {
		return pub.getAPIs(offset, limit, query, getToken());
	}
	
	@GetMapping("/mgmt/apis/{apiId}")
	public @ResponseBody API getAPI(@PathVariable String apiId) {
		API api = pub.getAPI(apiId, getToken());
		
		if (api.getThumbnailUri() != null) {
			api.setThumbnailUri("/mgmt"+api.getThumbnailUri());
		}
		return api;
	}
	
	@GetMapping("/mgmt/apis/{apiId}/thumbnail")
	public @ResponseBody byte[] getAPI(@PathVariable String apiId, HttpServletResponse res) {
		API api = pub.getAPI(apiId, getToken());
		res.setContentType(MediaType.IMAGE_JPEG_VALUE);
		if (api.getThumbnailUri() != null) return pub.getAPIThumbnail(apiId, getToken());
		return null;
	}

	@GetMapping("/mgmt/apis/{apiId}/subscriptions")
	public @ResponseBody DataList<Subscription> getAPISubscriptions(
			@PathVariable String apiId,
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit) 
	{
		return pub.getSubscriptions(apiId, offset, limit, getToken());
	}

	@PutMapping("/mgmt/apis/{apiId}/userroles")
	public @ResponseBody List<String> updateRoles(@PathVariable String apiId, @RequestBody RoleModel roleModel) throws AxisFault, RemoteException, TenantMgtAdminServiceExceptionException, RemoteUserStoreManagerServiceUserStoreExceptionException 
	{
		String name = "", domain = "";
		isService.updateRoles(roleModel, name, domain);
		return pub.getUserAPIRoles(apiId, name, domain, getToken());
	}
	/**
	 * @return 
	 * @throws Exception 
	 */
	private String getToken() {
		// TODO change
		return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
	}

	@RequestMapping("/apimanager/token")
	public @ResponseBody
	String createToken() {
		try {
			return tokenEmitter.createToken();
		} catch (Exception e) {
			throw new AccessDeniedException("Inusfficies API Manager rights");
		}
	}


}
