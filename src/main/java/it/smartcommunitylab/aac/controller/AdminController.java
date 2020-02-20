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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.model.ApprovalData;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.IdPData;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Response.RESPONSE;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Access to the administration resources.
 * @author raman
 *
 */
@ApiIgnore
@Controller
public class AdminController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private ServiceManager serviceManager;
	
	@RequestMapping("/admin/approvals")
	public @ResponseBody Response getApprovals() {
		Response result = new Response();
		
		List<ClientDetailsEntity> clients = clientDetailsRepository.findAll();
		List<ApprovalData> list = new ArrayList<ApprovalData>();
		for (ClientDetailsEntity e : clients) {
			ClientAppInfo info = ClientAppInfo.convert(e.getAdditionalInformation());
			if (info.getScopeApprovals() != null && !info.getScopeApprovals().isEmpty()) {
				ApprovalData data = new ApprovalData();
				data.setClientId(e.getClientId());
				data.setName(info.getName());
				data.setOwner(userRepository.findOne(e.getDeveloperId()).toString());
				data.setScopes(new ArrayList<ServiceScopeDTO>());
				for (String rId : info.getScopeApprovals().keySet()) {
					ServiceScopeDTO resource = serviceManager.getServiceScopeDTO(rId);
					data.getScopes().add(resource);
				}
				list.add(data);
			}
		}
		result.setData(list);
		return result;
	}

	@RequestMapping("/admin/idps")
	public @ResponseBody Response getIdPs() {
		Response result = new Response();
		
		List<ClientDetailsEntity> clients = clientDetailsRepository.findAll();
		List<IdPData> list = new ArrayList<IdPData>();
		for (ClientDetailsEntity e : clients) {
			ClientAppInfo info = ClientAppInfo.convert(e.getAdditionalInformation());
			if (info.getIdentityProviders() != null && !info.getIdentityProviders().isEmpty()) {
				User user = userRepository.findOne(e.getDeveloperId());
				if (user == null) continue;
				
				IdPData data = new IdPData();
				data.setClientId(e.getClientId());
				data.setName(info.getName());
				data.setOwner(user.toString());
				data.setIdps(new ArrayList<String>());
				for (String key : info.getIdentityProviders().keySet()) {
					Integer value = info.getIdentityProviders().get(key);
					if (ClientAppInfo.REQUESTED == value) {
						data.getIdps().add(key);
					}
				}
				if (!data.getIdps().isEmpty()) list.add(data);
			}
		}
		result.setData(list);
		return result;
	}
	
	@RequestMapping(value="/admin/approvals/{clientId}", method=RequestMethod.POST)
	public @ResponseBody Response approve(@PathVariable String clientId) throws Exception {
		ClientDetailsEntity e = clientDetailsRepository.findByClientId(clientId);
		ClientAppInfo info = ClientAppInfo.convert(e.getAdditionalInformation());
		if (!info.getScopeApprovals().isEmpty()) {
			Set<String> newScopeSet = new HashSet<String>();
			if (e.getScope() != null) newScopeSet.addAll(e.getScope());
			for (String rId : info.getScopeApprovals().keySet()) {
				ServiceScopeDTO resource = serviceManager.getServiceScopeDTO(rId);
				newScopeSet.add(resource.getScope());
			}
			e.setResourceIds(StringUtils.collectionToCommaDelimitedString(serviceManager.findServiceIdsByScopes(newScopeSet)));
			e.setScope(StringUtils.collectionToCommaDelimitedString(newScopeSet));
			info.setScopeApprovals(Collections.<String,Boolean>emptyMap());
			e.setAdditionalInformation(info.toJson());
			clientDetailsRepository.save(e);
		}
		return getApprovals();
	}

	@RequestMapping(value="/admin/idps/{clientId}", method=RequestMethod.POST)
	public @ResponseBody Response approveIdP(@PathVariable String clientId) throws Exception {
		ClientDetailsEntity e = clientDetailsRepository.findByClientId(clientId);
		ClientAppInfo info = ClientAppInfo.convert(e.getAdditionalInformation());
		if (!info.getIdentityProviders().isEmpty()) {
			for (String key : info.getIdentityProviders().keySet()) {
				info.getIdentityProviders().put(key, ClientAppInfo.APPROVED);
			}
			e.setAdditionalInformation(info.toJson());
			clientDetailsRepository.save(e);
		}
		return getIdPs();
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
