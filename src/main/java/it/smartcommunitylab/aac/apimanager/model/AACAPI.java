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

package it.smartcommunitylab.aac.apimanager.model;

import java.util.Set;

import it.smartcommunitylab.aac.wso2.model.API;

/**
 * @author raman
 *
 */
public class AACAPI extends API {

	private Set<String> applicationRoles;

	
	public AACAPI() {
		super();
	}

	public AACAPI(API api) {
		super();
		this.setApiDefinition(api.getApiDefinition());
		this.setContext(api.getContext());
		this.setDescription(api.getDescription());
		this.setEndpointConfig(api.getEndpointConfig());
		this.setId(api.getId());
		this.setIsDefaultVersion(api.getIsDefaultVersion());
		this.setName(api.getName());
		this.setProvider(api.getProvider());
		this.setResponseCaching(api.getResponseCaching());
		this.setStatus(api.getStatus());
		this.setThumbnailUri(api.getThumbnailUri());
		this.setTiers(api.getTiers());
		this.setTransport(api.getTransport());
		this.setVersion(api.getVersion());
		this.setVisibility(api.getVisibility());
		this.setVisibleRoles(api.getVisibleRoles());
		this.setVisibleTenants(api.getVisibleTenants());
		this.setWsdlUri(api.getWsdlUri());
		
	}

	public Set<String> getApplicationRoles() {
		return applicationRoles;
	}

	public void setApplicationRoles(Set<String> applicationRoles) {
		this.applicationRoles = applicationRoles;
	}
	
}
