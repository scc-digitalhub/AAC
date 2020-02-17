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

package it.smartcommunitylab.aac.dto;

import java.util.List;

import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;

/**
 * @author raman
 *
 */
public class ConnectedAppProfile {

	private String clientId;
	private String appName;
	private List<ServiceScopeDTO> scopes;
	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}
	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	/**
	 * @return the appName
	 */
	public String getAppName() {
		return appName;
	}
	/**
	 * @param appName the appName to set
	 */
	public void setAppName(String appName) {
		this.appName = appName;
	}
	/**
	 * @return the resources
	 */
	public List<ServiceScopeDTO> getScopes() {
		return scopes;
	}
	/**
	 * @param resources the resources to set
	 */
	public void setScopes(List<ServiceScopeDTO> scopes) {
		this.scopes = scopes;
	}
	
}
