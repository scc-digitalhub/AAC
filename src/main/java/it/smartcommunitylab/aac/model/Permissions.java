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

package it.smartcommunitylab.aac.model;

import java.util.List;
import java.util.Map;

import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;

/**
 * Permissions descriptor with client app own scope properties, services, scopes available,
 * resource approval statues, and scopes selected by the client
 * @author raman
 *
 */
public class Permissions {

	private ServiceDTO service;
	private List<ServiceScopeDTO> availableScopes;
	private Map<String,Boolean> selectedScopes;
	
	private Map<String,Integer> scopeApprovals;

	/**
	 * @return the service
	 */
	public ServiceDTO getService() {
		return service;
	}
	/**
	 * @param service the service to set
	 */
	public void setService(ServiceDTO service) {
		this.service = service;
	}
	/**
	 * @return the selectedScopes
	 */
	public Map<String, Boolean> getSelectedScopes() {
		return selectedScopes;
	}
	/**
	 * @return the availableScopes
	 */
	public List<ServiceScopeDTO> getAvailableScopes() {
		return availableScopes;
	}
	/**
	 * @param availableScopes the availableScopes to set
	 */
	public void setAvailableScopes(List<ServiceScopeDTO> availableScopes) {
		this.availableScopes = availableScopes;
	}
	/**
	 * @param selectedScopes the selectedScopes to set
	 */
	public void setSelectedScopes(Map<String, Boolean> selectedScopes) {
		this.selectedScopes = selectedScopes;
	}
	/**
	 * @return the scopeApprovals
	 */
	public Map<String, Integer> getScopeApprovals() {
		return scopeApprovals;
	}
	/**
	 * @param scopeApprovals the scopeApprovals to set
	 */
	public void setScopeApprovals(Map<String, Integer> scopeApprovals) {
		this.scopeApprovals = scopeApprovals;
	}
}
