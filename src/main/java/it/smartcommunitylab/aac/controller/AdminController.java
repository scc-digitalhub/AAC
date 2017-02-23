/**
 *    Copyright 2012-2013 Trento RISE
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import it.smartcommunitylab.aac.model.ApprovalData;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.IdPData;
import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Response.RESPONSE;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.ResourceRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * Access to the administration resources.
 * @author raman
 *
 */
@Controller
public class AdminController {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private ResourceRepository resourceRepository;

	private static final Log logger = LogFactory.getLog(AdminController.class);
	
	@RequestMapping("/admin/approvals")
	public @ResponseBody Response getApprovals() {
		Response result = new Response();
		result.setResponseCode(RESPONSE.OK);
		
		try {
			List<ClientDetailsEntity> clients = clientDetailsRepository.findAll();
			List<ApprovalData> list = new ArrayList<ApprovalData>();
			for (ClientDetailsEntity e : clients) {
				ClientAppInfo info = ClientAppInfo.convert(e.getAdditionalInformation());
				if (info.getResourceApprovals() != null && !info.getResourceApprovals().isEmpty()) {
					ApprovalData data = new ApprovalData();
					data.setClientId(e.getClientId());
					data.setName(info.getName());
					data.setOwner(userRepository.findOne(e.getDeveloperId()).toString());
					data.setResources(new ArrayList<Resource>());
					for (String rId : info.getResourceApprovals().keySet()) {
						Resource resource = resourceRepository.findOne(Long.parseLong(rId));
						data.getResources().add(resource);
					}
					list.add(data);
				}
			}
			result.setData(list);
		} catch (Exception e) {
			e.printStackTrace();
			result.setResponseCode(RESPONSE.ERROR);
			result.setErrorMessage(e.getMessage());
		}
		return result;
	}

	@RequestMapping("/admin/idps")
	public @ResponseBody Response getIdPs() {
		Response result = new Response();
		result.setResponseCode(RESPONSE.OK);
		
		try {
			List<ClientDetailsEntity> clients = clientDetailsRepository.findAll();
			List<IdPData> list = new ArrayList<IdPData>();
			for (ClientDetailsEntity e : clients) {
				ClientAppInfo info = ClientAppInfo.convert(e.getAdditionalInformation());
				if (info.getIdentityProviders() != null && !info.getIdentityProviders().isEmpty()) {
					IdPData data = new IdPData();
					data.setClientId(e.getClientId());
					data.setName(info.getName());
					data.setOwner(userRepository.findOne(e.getDeveloperId()).toString());
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
		} catch (Exception e) {
			e.printStackTrace();
			result.setResponseCode(RESPONSE.ERROR);
			result.setErrorMessage(e.getMessage());
		}
		return result;
	}
	
	@RequestMapping(value="/admin/approvals/{clientId}", method=RequestMethod.POST)
	public @ResponseBody Response approve(@PathVariable String clientId) {
		try {
			ClientDetailsEntity e = clientDetailsRepository.findByClientId(clientId);
			ClientAppInfo info = ClientAppInfo.convert(e.getAdditionalInformation());
			if (!info.getResourceApprovals().isEmpty()) {
				Set<String> idSet = new HashSet<String>();
				if (e.getResourceIds() != null) idSet.addAll(e.getResourceIds());
				Set<String> uriSet = new HashSet<String>();
				if (e.getScope() != null) uriSet.addAll(e.getScope());
				for (String rId : info.getResourceApprovals().keySet()) {
					Resource resource = resourceRepository.findOne(Long.parseLong(rId));
					idSet.add(rId);
					uriSet.add(resource.getResourceUri());
				}
				e.setResourceIds(StringUtils.collectionToCommaDelimitedString(idSet));
				e.setScope(StringUtils.collectionToCommaDelimitedString(uriSet));
				info.setResourceApprovals(Collections.<String,Boolean>emptyMap());
				e.setAdditionalInformation(info.toJson());
				clientDetailsRepository.save(e);
			}
			return getApprovals();
		} catch (Exception e) {
			Response result = new Response();
			result.setResponseCode(RESPONSE.ERROR);
			result.setErrorMessage(e.getMessage());
			return result;
		}
	}

	@RequestMapping(value="/admin/idps/{clientId}", method=RequestMethod.POST)
	public @ResponseBody Response approveIdP(@PathVariable String clientId) {
		try {
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
		} catch (Exception e) {
			Response result = new Response();
			result.setResponseCode(RESPONSE.ERROR);
			result.setErrorMessage(e.getMessage());
			return result;
		}
	}

}
